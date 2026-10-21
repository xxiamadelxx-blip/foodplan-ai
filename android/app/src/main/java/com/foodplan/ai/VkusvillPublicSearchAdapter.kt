package com.foodplan.ai

import com.foodplan.domain.NeutralRetailQueries
import com.foodplan.domain.RetailerProductMatcher
import com.foodplan.domain.ShoppingNeed
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * Automated on-device read-only public search. No AI, credentials or private APIs.
 * Does NOT follow product detail /goods/ pages (the retailer can disallow those).
 * Results are NAME-ONLY candidate references: never package, price, allergy or stock proof.
 */
internal data class PublicSearchCandidate(
    val ingredientId: String,
    val publicProductId: String,
    val title: String,
    val publicUrl: String
)
internal data class PublicSearchDiscovery(
    val candidates: Map<String, List<PublicSearchCandidate>>,
    val queriedIngredients: Int,
    val totalIngredients: Int,
    val complete: Boolean,
    val message: String?
)

/** Injectable HTTP and delay are for independent Android tests; real scanner respects robots. */
internal class VkusvillPublicSearchAdapter(
    private val fetch: suspend (String) -> ByteArray = ::fetchAllowedPublicPage,
    private val waitBeforeRequest: suspend (Long) -> Unit = { delay(it) }
) {
    private val base = "https://vkusvill.ru"
    private val robotsUrl = "$base/robots.txt"
    private val skuPath = Regex("^/goods/[a-z0-9-]+-(\\d+)/$")
    private val maxHtmlSize = 3_000_000

    suspend fun scan(shopping: List<ShoppingNeed>): PublicSearchDiscovery {
        val ordered = NeutralRetailQueries.fromShopping(shopping)
            .distinctBy { it.ingredientId }
            .let { needs ->
                listOfNotNull(needs.firstOrNull { it.ingredientId == "chicken" }) +
                    needs.filterNot { it.ingredientId == "chicken" }
                        .sortedBy { it.ingredientId }
            }
        if (ordered.isEmpty()) return PublicSearchDiscovery(emptyMap(), 0, 0, true, null)
        val output = linkedMapOf<String, List<PublicSearchCandidate>>()
        val policyBytes = try { fetch(robotsUrl) } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return PublicSearchDiscovery(output, 0, ordered.size, false,
                "Магазин сейчас не предоставил доступ к своему поиску.")
        }
        val pauseMs = safeRobotsDelay(policyBytes)
            ?: return PublicSearchDiscovery(output, 0, ordered.size, false,
                "Автоматический поиск ограничен правилами магазина.")
        var attempted = 0
        for (need in ordered) {
            waitBeforeRequest(pauseMs)
            val url = "$base/search/?q=" + URLEncoder.encode(need.searchTerms.first(), "UTF-8")
            val raw = try { fetch(url) } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return PublicSearchDiscovery(output, attempted, ordered.size, false,
                    "Не удалось получить все предложения магазина. Цены не подтверждены.")
            }
            attempted++
            if (raw.size > maxHtmlSize) {
                return PublicSearchDiscovery(output, attempted, ordered.size, false,
                    "Ответ магазина слишком большой для безопасной обработки.")
            }
            val doc = Jsoup.parse(raw.toString(Charsets.UTF_8), base)
            val text = doc.body().text().lowercase()
            if (listOf("captcha", "access denied", "verify you are human",
                    "checking your browser", "доступ ограничен",
                    "проверьте, что вы не робот", "подозрительная активность")
                    .any { it in text }) {
                return PublicSearchDiscovery(output, attempted, ordered.size, false,
                    "Магазин ограничил автоматический поиск. Остальные запросы остановлены.")
            }
            val distinct = linkedMapOf<String, PublicSearchCandidate>()
            val ambiguousIds = mutableSetOf<String>()
            for (anchor in doc.select("a[href]")) {
                val target = parseSkuLink(anchor.attr("href")) ?: continue
                val id = skuPath.matchEntire(target.path)?.groupValues?.get(1) ?: continue
                if (id in ambiguousIds) continue
                val name = anchor.text().replace(Regex("\\s+"), " ").trim()
                if (name.length !in 5..250) continue
                if (!RetailerProductMatcher.matchesIngredient(need.ingredientId, name))
                    continue
                val candidate = PublicSearchCandidate(
                    need.ingredientId, id, name, base + target.path
                )
                if (id in distinct && distinct[id]?.title != name) {
                    distinct.remove(id)
                    ambiguousIds.add(id)
                } else if (id !in distinct && distinct.size < 50) {
                    distinct[id] = candidate
                }
            }
            output[need.ingredientId] = distinct.values.sortedWith(
                compareBy<PublicSearchCandidate> { it.title }.thenBy { it.publicProductId }
            )
        }
        return PublicSearchDiscovery(output, attempted, ordered.size, true, null)
    }

    private fun parseSkuLink(link: String): URI? {
        if (link.isBlank() || link.contains('\\') || link.contains('@')) return null
        val uri = try { URI(base).resolve(link) } catch (_: Exception) { return null }
        if (uri.scheme != "https" || uri.host != "vkusvill.ru" ||
            uri.port != -1 || uri.userInfo != null ||
            uri.rawQuery != null || uri.rawFragment != null) return null
        if (!skuPath.matches(uri.path)) return null
        return uri
    }

    /** Require explicit star-group permission; respect 2s minimum delay. */
    internal fun safeRobotsDelay(data: ByteArray): Long? {
        if (data.size > 100_000) return null
        val content = data.toString(Charsets.UTF_8)
        var active = false
        var allowed = false
        var delaySeconds = 2.0
        for (line in content.lineSequence()) {
            val cleaned = line.substringBefore('#').trim()
            val index = cleaned.indexOf(':')
            if (index == -1) continue
            val key = cleaned.substring(0, index).trim().lowercase()
            val value = cleaned.substring(index + 1).trim()
            when (key) {
                "user-agent" -> active = value == "*"
                "allow" -> if (active && value == "/search/?q=*") allowed = true
                "disallow" -> if (active && (value == "/search/" ||
                    value == "/search/?q=*" || value == "/search/?q=")) return null
                "crawl-delay" -> if (active) {
                    val candidate = value.toDoubleOrNull() ?: return null
                    if (!candidate.isFinite() || candidate !in 0.0..60.0) return null
                    delaySeconds = maxOf(2.0, candidate)
                }
            }
        }
        return if (allowed) (delaySeconds * 1000).toLong() else null
    }
}

private suspend fun fetchAllowedPublicPage(url: String): ByteArray = withContext(Dispatchers.IO) {
    val uri = URI(url)
    val path = uri.path
    if (uri.scheme != "https" || uri.host != "vkusvill.ru" ||
        uri.port != -1 || uri.userInfo != null ||
        !(path == "/robots.txt" && uri.query == null ||
          path == "/search/" && uri.rawQuery?.startsWith("q=") == true))
        throw IllegalArgumentException("Only official robots and public search are permitted")
    val conn = URL(url).openConnection() as HttpURLConnection
    try {
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 12_000
        conn.readTimeout = 12_000
        conn.setRequestProperty("User-Agent", "FoodPlanAI-public-search-audit/0.1 (read-only)")
        if (conn.responseCode != 200) throw IllegalStateException("Public endpoint unavailable")
        val buffer = ByteArray(8192)
        val output = ByteArrayOutputStream()
        val max = if (path == "/robots.txt") 100_000 else 3_000_000
        conn.inputStream.use { stream ->
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (output.size() + count > max)
                    throw IllegalStateException("Public response size limit exceeded")
                output.write(buffer, 0, count)
            }
        }
        output.toByteArray()
    } finally {
        conn.disconnect()
    }
}
