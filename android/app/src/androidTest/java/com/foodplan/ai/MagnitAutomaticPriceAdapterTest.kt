package com.foodplan.ai

import com.foodplan.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Pure fixture pages. No real network in Android CI. */
class MagnitAutomaticPriceAdapterTest {
    private val robots = "User-agent: *\nDisallow: /private/\nAllow: /product/\nCrawl-delay: 2\n".toByteArray()

    private fun need(id: String, grams: Int = 901) =
        ShoppingNeed(OfflineRecipes.ingredients.getValue(id), grams, 2, grams, 0)

    @Test fun officialPathsCoverKnownCatalogButDoNotContainAnyPrices() {
        assertEquals(35, MagnitKnownProductPaths.knownIngredientIds.size)
        assertEquals(MagnitKnownSkuCatalog.urls.keys,
            MagnitKnownProductPaths.knownIngredientIds)
        // The Android foreground scanner must use ALL known paths, not a
        // narrower duplicate catalog that accidentally omits gross-pack items.
        for ((id, officialUrl) in MagnitKnownSkuCatalog.urls) {
            assertEquals("Wrong on-device SKU URL for $id", officialUrl,
                MagnitKnownProductPaths.urlFor(id))
        }
        assertEquals(MagnitKnownSkuCatalog.urlFor("banana"),
            MagnitKnownProductPaths.urlFor("banana"))
        assertEquals(MagnitKnownSkuCatalog.urlFor("chickpeas"),
            MagnitKnownProductPaths.urlFor("chickpeas"))
        assertNull(MagnitKnownProductPaths.expectedGrams("banana"))
        assertNull(MagnitKnownProductPaths.expectedGrams("chickpeas"))
        assertEquals(null, MagnitKnownProductPaths.urlFor("berries"))
        assertEquals(null, MagnitKnownProductPaths.urlFor("pork"))
        assertEquals(800, MagnitKnownProductPaths.expectedGrams("rice"))
        assertNull(MagnitKnownProductPaths.expectedGrams("egg"))
        assertTrue(MagnitKnownProductPaths.urlFor("rice")!!
            .startsWith("https://magnit.ru/product/"))
    }

    @Test fun robotsDirectiveGateHonorsLongestRuleAndRestrictsHost() {
        val policy = MagnitRobotsPolicy.parse((
            "User-agent: *\nDisallow: /\nAllow: /product/\nDisallow: /product/private/\n"
        ).toByteArray())
        assertNotNull(policy)
        assertTrue(policy!!.allows(java.net.URI(MagnitKnownProductPaths.urlFor("rice"))))
        assertFalse(policy.allows(java.net.URI("https://magnit.ru/product/private/secret")))
        assertFalse(policy.allows(java.net.URI("https://example.com/product/rice")))
        assertFalse(policy.allows(java.net.URI("https://magnit.ru/catalog/foo")))
        assertNull(MagnitRobotsPolicy.parse("No user-agent".toByteArray()))
    }

    @Test fun parsesOnlyVisiblePriceAfterOfficialProductTitleAndExactMass() {
        val url = requireNotNull(MagnitKnownProductPaths.urlFor("rice"))
        val html = ("<section>1,00 ₽</section><h1>Рис длиннозерный 800г</h1>" +
            "<del>900,00 ₽</del><p>49,99 ₽</p>" +
            "<h2>Похожие</h2><p>3,00 ₽</p>").toByteArray()
        val product = MagnitPublicHtmlPriceParser.parse(html,url,"rice")
        assertNotNull(product)
        assertEquals(4999L, product!!.priceKopeks)
        assertEquals(800, product.packGrams)
        assertNull(MagnitPublicHtmlPriceParser.parse(html,url,"buckwheat"))
        assertNull(MagnitPublicHtmlPriceParser.parse(
            "<h1>Рис длиннозерный 400г</h1><p>49,99 ₽</p>".toByteArray(),url,"rice"))
        assertNull(MagnitPublicHtmlPriceParser.parse(
            "<h1>Рис длиннозерный 800г</h1><h2>Похожие</h2><p>49,99 ₽</p>"
                .toByteArray(),url,"rice"))
        assertNull(MagnitPublicHtmlPriceParser.parse(
            "<h1>Рис длиннозерный 800г</h1><p hidden>49,99 ₽</p>"
                .toByteArray(),url,"rice"))
    }

    @Test fun cartonAndBottleHaveObservablePriceButNeverEdibleGramCount() {
        val eggUrl = requireNotNull(MagnitKnownProductPaths.urlFor("egg"))
        val oilUrl = requireNotNull(MagnitKnownProductPaths.urlFor("oil"))
        val egg = MagnitPublicHtmlPriceParser.parse(
            "<h1>Яйцо куриное С1 10шт</h1><p>85,39 ₽</p>".toByteArray(),eggUrl,"egg")
        assertNotNull(egg)
        assertNull(egg!!.packGrams)
        assertEquals(PurchasePack(PurchaseUnit.PIECE,10), egg.purchasePack)
        val oil = MagnitPublicHtmlPriceParser.parse(
            "<h1>Масло подсолнечное 900мл</h1><p>125,99 ₽</p>"
                .toByteArray(),oilUrl,"oil")
        assertNotNull(oil)
        assertNull(oil!!.packGrams)
        assertEquals(PurchasePack(PurchaseUnit.MILLILITRE,900),oil.purchasePack)
        val quote = RetailWeekQuoter.build(
            AnonymousRetailPlanner.build(listOf(need("egg"),need("oil"))),
            listOf(
                RetailProductEvidence(egg,"Магнит","Санкт-Петербург",
                    System.currentTimeMillis(),false),
                RetailProductEvidence(oil,"Магнит","Санкт-Петербург",
                    System.currentTimeMillis(),false)
            ),"Магнит","Санкт-Петербург",356,System.currentTimeMillis())
        assertNull(quote.completeEstimateKopeks)
        assertEquals(2,quote.missingIngredientIds.size)
        assertEquals(0,quote.pricedIngredientCount)
        assertEquals(2,quote.lines.count { it.observedPackPriceKopeks != null })
    }

    @Test fun scannerUsesRobotsOnceAndAutomaticallyEmitsTrueMassOffer() = runBlocking {
        val calls = mutableListOf<String>()
        val url = requireNotNull(MagnitKnownProductPaths.urlFor("rice"))
        val scan = MagnitAutomaticPriceAdapter(
            fetch = { link ->
                calls += link
                when(link) {
                    "https://magnit.ru/robots.txt" -> robots
                    url -> "<h1>Рис длиннозерный 800г</h1><span>49,99 ₽</span>"
                        .toByteArray()
                    else -> error("Unexpected request")
                }
            }, pause = {})
        val result = scan.scan(listOf(need("rice")),"Санкт-Петербург")
        assertEquals(2,calls.size)
        assertEquals("https://magnit.ru/robots.txt",calls.first())
        assertEquals(1,result.observations.size)
        assertFalse(result.sourceAccessBlocked)
        val stored = AutomaticRetailScanPolicy.verifiedMassOffers(
            listOf(need("rice")),result.observations,
            "Санкт-Петербург",356,System.currentTimeMillis())
        assertEquals(1,stored.size)
        assertEquals(800,stored.single().packGrams)
        assertEquals(4999L,stored.single().priceKopeks)
    }

    @Test fun blockedHostStopsNetworkAndKeepsAllMissingUnknown() = runBlocking {
        val calls = mutableListOf<String>()
        val scan = MagnitAutomaticPriceAdapter(
            fetch = { link ->
                calls += link
                if (link.endsWith("robots.txt")) robots
                else throw RetailHttpRefused(403)
            },pause = {})
        val result=scan.scan(listOf(need("rice"),need("egg"),need("oil")),
            "Санкт-Петербург")
        assertEquals(2,calls.size)
        assertTrue(result.sourceAccessBlocked)
        assertEquals(3,result.unavailableIngredientIds.size)
        assertTrue(result.observations.isEmpty())
    }

    @Test fun robotsRefusalDoesNotRequestAnyStoreProducts() = runBlocking {
        val calls = mutableListOf<String>()
        val scanner = MagnitAutomaticPriceAdapter(fetch = { link ->
            calls += link
            throw RetailHttpRefused(403)
        },pause = {})
        val result=scanner.scan(listOf(need("rice"),need("egg")),
            "Санкт-Петербург")
        assertEquals(1,calls.size)
        assertTrue(result.sourceAccessBlocked)
        assertTrue(result.observations.isEmpty())
        assertEquals(2,result.unavailableIngredientIds.size)
    }

    @Test fun automaticScannerReadsEveryKnownUnitWithoutInventingEdibleWeight() = runBlocking {
        val ids = listOf("rice","egg","banana","chickpeas")
        val known = ids.associateWith { requireNotNull(MagnitKnownProductPaths.urlFor(it)) }
        val pages = mapOf(
            "rice" to "<h1>Рис длиннозерный 800г</h1><p>49,99 ₽</p>",
            "egg" to "<h1>Яйцо куриное С1 10шт</h1><p>85,39 ₽</p>",
            "banana" to "<h1>Бананы 1кг</h1><p>129,99 ₽</p>",
            "chickpeas" to "<h1>Нут Green Ribbon 400г</h1><p>119,99 ₽</p>"
        )
        val requests = mutableListOf<String>()
        val adapter = MagnitAutomaticPriceAdapter(
            fetch = { url ->
                requests += url
                if (url == "https://magnit.ru/robots.txt") robots
                else {
                    val id = known.entries.singleOrNull { it.value == url }?.key
                        ?: error("Unexpected public URL: $url")
                    requireNotNull(pages[id]).toByteArray(Charsets.UTF_8)
                }
            }, pause = {}
        )
        val needs = ids.map { need(it) }
        val result = adapter.scan(needs,"Санкт-Петербург")
        assertEquals(5,requests.size) // 1 robots + 4 public product pages
        assertEquals(4,result.observations.size)
        assertTrue(result.unavailableIngredientIds.isEmpty())
        assertFalse(result.sourceAccessBlocked)

        val quote = RetailWeekQuoter.build(
            AnonymousRetailPlanner.build(needs),
            result.observations,
            "Магнит", "Санкт-Петербург", 356,
            System.currentTimeMillis()
        )
        assertEquals(1,quote.pricedIngredientCount)
        assertEquals(4,quote.requiredIngredientCount)
        assertEquals(3,quote.missingIngredientIds.size)
        assertNull(quote.completeEstimateKopeks)
        assertEquals(3,quote.lines.count { it.observedPackPriceKopeks != null })
        for (id in listOf("egg","banana","chickpeas")) {
            val line = quote.lines.single { it.ingredientId == id }
            assertNull(line.packages)
            assertNull(line.estimatedCostKopeks)
            assertNotNull(line.observedPackPriceKopeks)
        }
    }
}
