package com.foodplan.ai

import com.foodplan.domain.*
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/** A public-source refusal is NOT a zero-ruble offer. */
internal class RetailHttpRefused(val status: Int) : Exception("HTTP_$status")

data class MagnitAutoPriceResult(
    val observations: List<RetailProductEvidence>,
    val requestedCount: Int,
    val unavailableIngredientIds: Set<String>,
    val message: String?,
    val sourceAccessBlocked: Boolean
)

/** Conservative robots check for known public /product/ URLs. */
internal class MagnitRobotsPolicy private constructor(
    private val rules: List<Pair<Boolean, String>>,
    val delayMs: Long
) {
    fun allows(uri: URI): Boolean {
        if (uri.scheme != "https" || uri.host != "magnit.ru" ||
            uri.port != -1 || uri.userInfo != null ||
            uri.rawQuery != null || uri.rawFragment != null) return false
        val path = uri.rawPath ?: return false
        var best = -1
        var decision = true
        for ((allow, pattern) in rules) {
            if (pattern.isBlank()) continue
            val anchored = pattern.endsWith("$")
            val fragment = if (anchored) pattern.dropLast(1) else pattern
            val expression = buildString {
                append('^')
                for (c in fragment) {
                    if (c == '*') append(".*")
                    else append(Regex.escape(c.toString()))
                }
                if (anchored) append('$')
            }
            if (Regex(expression).containsMatchIn(path)) {
                val score = fragment.replace("*", "").length
                if (score > best || score == best && allow) {
                    best = score
                    decision = allow
                }
            }
        }
        return decision
    }

    companion object {
        fun parse(raw: ByteArray): MagnitRobotsPolicy? {
            if (raw.isEmpty() || raw.size > 100_000) return null
            val text = raw.toString(Charsets.UTF_8)
            if ('\uFFFD' in text) return null
            data class Group(val agents: MutableList<String> = mutableListOf(),
                             val rules: MutableList<Pair<Boolean, String>> = mutableListOf(),
                             var delay: Double = 2.0)
            val groups = mutableListOf<Group>()
            var group = Group()
            var hasRules = false
            for (line in text.lineSequence()) {
                val row = line.substringBefore('#').trim()
                val colon = row.indexOf(':')
                if (colon == -1) continue
                val field = row.substring(0, colon).trim().lowercase()
                val value = row.substring(colon + 1).trim()
                when (field) {
                    "user-agent" -> {
                        if (hasRules) {
                            if (group.agents.isNotEmpty()) groups += group
                            group = Group()
                            hasRules = false
                        }
                        group.agents += value.lowercase()
                    }
                    "allow", "disallow" -> {
                        if (group.agents.isNotEmpty()) {
                            group.rules += (field == "allow") to value
                            hasRules = true
                        }
                    }
                    "crawl-delay" -> {
                        if (group.agents.isNotEmpty()) {
                            group.delay = value.toDoubleOrNull() ?: return null
                            if (!group.delay.isFinite() || group.delay !in 0.0..60.0)
                                return null
                            hasRules = true
                        }
                    }
                }
            }
            if (group.agents.isNotEmpty()) groups += group
            // Prefer an explicit FoodPlan user-agent policy, then *.
            val named = groups.filter { it.agents.any { a ->
                a == "foodplanai" || a == "foodplanai-android"
            } }
            val selected = if (named.isNotEmpty()) named else
                groups.filter { "*" in it.agents }
            if (selected.isEmpty()) return null
            val delay = maxOf(2.0, selected.maxOf { it.delay })
            return MagnitRobotsPolicy(selected.flatMap { it.rules },
                (delay * 1000).toLong())
        }
    }
}

/** Pure deterministic extractor. It rejects script data and unrelated product prices. */
internal object MagnitPublicHtmlPriceParser {
    private val exactPrice = Regex("""^\d{1,6}(?:[.,]\d{1,2})?\s*₽$""")
    private val related = Regex(
        "^(похожие|похожие товары|рекомендуем|вам может понравиться|с этим товаром покупают)$",
        RegexOption.IGNORE_CASE
    )

    private fun isVisible(element: Element): Boolean {
        var node: Element? = element
        while (node != null) {
            if (node.hasAttr("hidden") || node.attr("aria-hidden") == "true") return false
            if (node.tagName() in setOf("script","style","template","del","s","strike"))
                return false
            val style = node.attr("style").replace(" ", "").lowercase()
            if ("display:none" in style || "visibility:hidden" in style) return false
            val cls = node.className().lowercase()
            if ("old-price" in cls || "price-old" in cls) return false
            node = node.parent()
        }
        return true
    }

    fun parse(html: ByteArray, url: String, ingredientId: String): ObservedProduct? {
        if (html.isEmpty() || html.size > 3_000_000) return null
        val knownUrl = MagnitKnownProductPaths.urlFor(ingredientId) ?: return null
        if (knownUrl != url) return null
        val source = html.toString(Charsets.UTF_8)
        if ('\uFFFD' in source) return null
        val doc = Jsoup.parse(source, url)
        if (doc.body()?.text()?.lowercase()?.let { text ->
                listOf("captcha", "checking your browser", "access denied",
                    "подозрительная активность", "доступ ограничен",
                    "verify you are human").any { it in text }
            } != false) return null
        val titleNode = doc.selectFirst("h1") ?: return null
        if (!isVisible(titleNode)) return null
        val title = titleNode.text().trim()
        if (title.length !in 5..240) return null
        val special = MagnitPackOnlySkuCatalog.forUrl(url)
        val grams = if (special == null) {
            val gramsExpected = MagnitKnownProductPaths.expectedGrams(ingredientId)
                ?: return null
            if (CatalogValueParser.packageGrams(title) != gramsExpected) return null
            if (!RetailerProductMatcher.matchesIngredient(ingredientId, title))
                return null
            gramsExpected
        } else null

        val elements = doc.body()?.getAllElements() ?: return null
        val startingAt = elements.indexOf(titleNode)
        if (startingAt < 0) return null
        var seenTextCandidates = 0
        var price: Long? = null
        for (idx in (startingAt + 1) until elements.size) {
            val item = elements[idx]
            if (!isVisible(item)) continue
            if (item.tagName() in setOf("h2","h3") && related.matches(item.text().trim()))
                break
            if (item.children().isNotEmpty()) continue
            val text = item.text().trim().replace('\u202f',' ').replace('\u200a',' ')
            if (text.isBlank()) continue
            seenTextCandidates++
            if (seenTextCandidates > 25) break
            if (exactPrice.matches(text)) {
                price = CatalogValueParser.parseDisplayedRubles(text)
                break
            }
        }
        if (price == null || price !in 1L..10_000_000L) return null
        val product = ObservedProduct(title, url, grams, price,
            ListingAvailability.UNKNOWN, purchasePack = special?.pack)
        return if (special == null || MagnitPackOnlySkuCatalog.matching(product) == special)
            product else null
    }
}

/**
 * Called automatically by the ordinary Android Products screen.
 * Only official, previously known public HTML pages, no private endpoints,
 * WebView clicks, user address, AI or synthetic fallback prices.
 */
internal class MagnitAutomaticPriceAdapter(
    private val fetch: suspend (String) -> ByteArray = ::downloadMagnitPublicHtml,
    private val pause: suspend (Long) -> Unit = { delay(it) }
) {
    suspend fun scan(shopping: List<ShoppingNeed>, city: String): MagnitAutoPriceResult {
        val ids = shopping.filter { it.grams > 0 }.map { it.ingredient.id }.distinct()
        if (ids.isEmpty()) return MagnitAutoPriceResult(emptyList(),0,emptySet(),null,false)
        val failures = mutableSetOf<String>()
        val observations = mutableListOf<RetailProductEvidence>()
        val robots = try {
            MagnitRobotsPolicy.parse(fetch("https://magnit.ru/robots.txt"))
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { null }
        if (robots == null) return MagnitAutoPriceResult(emptyList(),ids.size,
            ids.toSet(),"Каталог магазина сейчас недоступен. Цены неизвестны.",true)

        var blocked = false
        for (id in ids) {
            val url = MagnitKnownProductPaths.urlFor(id)
            if (url == null || !robots.allows(URI(url))) {
                failures += id
                continue
            }
            pause(robots.delayMs)
            try {
                val product = MagnitPublicHtmlPriceParser.parse(
                    fetch(url), url, id)
                if (product == null) failures += id
                else observations += RetailProductEvidence(
                    product,"Магнит",city,System.currentTimeMillis(),false)
            } catch (e: CancellationException) { throw e }
              catch (e: RetailHttpRefused) {
                failures += id
                if (e.status in setOf(401,403,429)) {
                    blocked = true
                    break
                }
            } catch (_: Exception) {
                failures += id
            }
        }
        val found = observations.map { product ->
            ids.firstOrNull { id ->
                MagnitKnownProductPaths.urlFor(id) == product.product.productUrl
            }
        }.filterNotNull().toSet()
        val missing = ids.toSet() - found
        return MagnitAutoPriceResult(
            observations,ids.size,missing,
            if (blocked) "Магазин ограничил доступ. Остальные цены неизвестны."
            else if (missing.isNotEmpty()) "Часть цен не удалось получить из каталога."
            else null,blocked)
    }
}

private suspend fun downloadMagnitPublicHtml(url: String): ByteArray =
    withContext(Dispatchers.IO) {
        val uri = URI(url)
        val robots = uri.path == "/robots.txt"
        if (uri.scheme != "https" || uri.host != "magnit.ru" ||
            uri.port != -1 || uri.userInfo != null || uri.query != null ||
            uri.fragment != null ||
            (!robots && !Regex("^/product/[A-Za-z0-9_-]+/?$").matches(uri.path)))
            throw IllegalArgumentException("Not an allowed official public URL")
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent","FoodPlanAI-Android/0.5 (read-only)")
            val code = conn.responseCode
            if (code != 200) throw RetailHttpRefused(code)
            val max = if (robots) 100_000 else 3_000_000
            val buffer = ByteArray(8192)
            val out = ByteArrayOutputStream()
            conn.inputStream.use { input ->
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (out.size() + n > max)
                        throw IllegalStateException("Official page response too large")
                    out.write(buffer,0,n)
                }
            }
            out.toByteArray()
        } finally { conn.disconnect() }
    }
