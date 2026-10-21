package com.foodplan.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foodplan.domain.OfflineRecipes
import com.foodplan.domain.ShoppingNeed
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** No real network: tests verify policy, 2s throttle, data and product filtering. */
@RunWith(AndroidJUnit4::class)
class VkusvillPublicSearchAdapterTest {
    private val robots = (
        "User-agent: *\n" +
        "Disallow: */?*\n" +
        "Disallow: */-*\n" +
        "Allow: /search/?q=*\n" +
        "Crawl-delay: 2\n"
    ).toByteArray()
    private val chicken = ShoppingNeed(OfflineRecipes.ingredients.getValue("chicken"), 900, 2)
    private val rice = ShoppingNeed(OfflineRecipes.ingredients.getValue("rice"), 500, 1)

    private val html = """
        <html><script><a href="/goods/fake-breast-9999/">Филе грудки цыпленка</a></script>
        <a href="/goods/file-grudki-tsyplenka-broylera-488/">
           <span>Филе грудки цыпленка-бройлера</span>
        </a>
        <a href="/goods/bedro-tsyplenka-19419/">Бедро цыпленка (филе бедра)</a>
        <a href="/goods/kurinoe-file-zapechennoe-34581/">Филе куриной грудки запеченное</a>
        <a href="/goods/file-ovoshchi-marinad-38215/">
           Филе грудки цыпленка-бройлера для запекания в маринаде
        </a>
        <a href="https://evil.test/goods/another-666/">Филе грудки цыпленка</a>
        <a href="/goods/offer-9988/?coupon=1">Филе грудки цыпленка</a>
        </html>
    """.trimIndent().toByteArray()

    @Test fun twoQueriesAreFetchedAfterRobotsWithPilotFirstAndNoProductPageRequests() = runBlocking {
        val urls = mutableListOf<String>()
        val delays = mutableListOf<Long>()
        val adapter = VkusvillPublicSearchAdapter(
            fetch = { url ->
                urls += url
                when {
                    url.endsWith("robots.txt") -> robots
                    url.contains("%D0%BA%D1%83") -> html
                    else -> """
                        <a href="/goods/ris-dlinnozernyy-sukhoy-900-g-22222/">
                          Рис длиннозерный сухой, 900 г</a>
                        <a href="/goods/ris-proparennyy-33333/">Рис пропаренный 900 г</a>
                    """.trimIndent().toByteArray()
                }
            },
            waitBeforeRequest = { delays += it }
        )
        val result = adapter.scan(listOf(rice, chicken))
        assertTrue(result.complete)
        assertEquals(2, result.queriedIngredients)
        assertEquals(2, result.totalIngredients)
        assertEquals(3, urls.size)
        assertEquals(listOf(2000L, 2000L), delays)
        assertTrue(urls[0].endsWith("/robots.txt"))
        assertTrue(urls[1].contains("%D0%BA%D1%83"))
        assertEquals(listOf("488"), result.candidates["chicken"]?.map { it.publicProductId })
        assertEquals(listOf("22222"), result.candidates["rice"]?.map { it.publicProductId })
        assertTrue(result.candidates.values.flatten().all {
            it.publicUrl.startsWith("https://vkusvill.ru/goods/")
        })
    }

    @Test fun explicitSearchDisallowOrMissingRobotsAllowStopsBeforeAnySearch() = runBlocking {
        for (bad in listOf(
            "User-agent: *\nDisallow: /search/\nAllow: /search/?q=*\n",
            "User-agent: *\nDisallow: /ajax/\n",
            "User-agent: *\nAllow: /search/?q=*\nCrawl-delay: 90\n"
        )) {
            val urls = mutableListOf<String>()
            val adapter = VkusvillPublicSearchAdapter(
                fetch = { url -> urls += url; bad.toByteArray() },
                waitBeforeRequest = { }
            )
            val result = adapter.scan(listOf(chicken))
            assertFalse(result.complete)
            assertEquals(0, result.queriedIngredients)
            assertEquals(1, urls.size)
            assertTrue(result.candidates.isEmpty())
        }
    }

    @Test fun captchaWithVisibleProductLinksNeverGetsApprovedOrContinuesToSecondCategory() = runBlocking {
        val urls = mutableListOf<String>()
        val adapter = VkusvillPublicSearchAdapter(
            fetch = { url ->
                urls += url
                if (url.endsWith("/robots.txt")) robots
                else ("<h1>Access denied CAPTCHA</h1>" +
                      html.toString(Charsets.UTF_8)).toByteArray()
            },
            waitBeforeRequest = { }
        )
        val result = adapter.scan(listOf(chicken, rice))
        assertFalse(result.complete)
        assertEquals(1, result.queriedIngredients)
        assertTrue(result.candidates.isEmpty())
        assertEquals(2, urls.size)
    }

    @Test fun networkFailureNeverInventsShoppingPriceOrRetriesForbiddenResponse() = runBlocking {
        val urls = mutableListOf<String>()
        val adapter = VkusvillPublicSearchAdapter(
            fetch = { url ->
                urls += url
                if (url.endsWith("/robots.txt")) robots else error("HTTP 429")
            },
            waitBeforeRequest = { }
        )
        val result = adapter.scan(listOf(chicken, rice))
        assertFalse(result.complete)
        assertEquals(0, result.queriedIngredients)
        assertEquals(2, urls.size)
        assertTrue(result.candidates.isEmpty())
    }

    @Test fun successfulHttpWithEmptyOrPlaceholderHtmlNeverCompletesRetailerScan() = runBlocking {
        val pages = listOf(
            ByteArray(0),
            "<html><body>Сервис временно недоступен</body></html>".toByteArray(),
            "<html><body><a href='/delivery/'>Личный кабинет</a></body></html>".toByteArray(),
            "<html><body><script>window.grocery=[]</script></body></html>".toByteArray()
        )
        for (page in pages) {
            val urls = mutableListOf<String>()
            val adapter = VkusvillPublicSearchAdapter(
                fetch = { url ->
                    urls += url
                    if (url.endsWith("robots.txt")) robots else page
                },
                waitBeforeRequest = { }
            )
            val result = adapter.scan(listOf(chicken, rice))
            assertFalse(result.complete)
            assertEquals(1, result.queriedIngredients)
            assertEquals(2, result.totalIngredients)
            assertTrue(result.candidates.isEmpty())
            assertEquals(2, urls.size) // Never continue on a 200 placeholder.
            assertTrue(result.message?.contains("неполный") == true)
        }
    }

    @Test fun laterSearchPlaceholderPreservesOnlyEarlierCandidateResults() = runBlocking {
        val urls = mutableListOf<String>()
        val adapter = VkusvillPublicSearchAdapter(
            fetch = { url ->
                urls += url
                when {
                    url.endsWith("robots.txt") -> robots
                    url.contains("%D0%BA%D1%83") -> html
                    else -> "<html><h1>Сервис временно недоступен</h1></html>".toByteArray()
                }
            },
            waitBeforeRequest = { }
        )
        val result = adapter.scan(listOf(chicken, rice))
        assertFalse(result.complete)
        assertEquals(2, result.queriedIngredients)
        assertEquals(listOf("488"),
            result.candidates["chicken"]?.map { it.publicProductId })
        assertFalse(result.candidates.containsKey("rice"))
        assertEquals(3, urls.size)
    }

    @Test fun genuineSearchWithOnlyUnrelatedProductsIsNotInventedAsMatchingProduct() = runBlocking {
        val unrelated = """<a href="/goods/ris-900-g-55555/">Рис 900 г</a>""".toByteArray()
        val adapter = VkusvillPublicSearchAdapter(
            fetch = { url -> if (url.endsWith("robots.txt")) robots else unrelated },
            waitBeforeRequest = { }
        )
        val result = adapter.scan(listOf(chicken))
        assertTrue(result.complete)
        assertEquals(emptyList<PublicSearchCandidate>(), result.candidates["chicken"])
    }

    @Test fun conflictingNamesUnderOneSkuIdAreRejected() = runBlocking {
        val ambiguous = """
        <a href="/goods/file-grudki-a-444/">Филе грудки цыпленка-бройлера</a>
        <a href="/goods/file-grudki-b-444/">Филе грудки цыпленка-бройлера на зерновом откорме</a>
        """.trimIndent().toByteArray()
        val adapter = VkusvillPublicSearchAdapter(
            fetch = { url -> if (url.endsWith("robots.txt")) robots else ambiguous },
            waitBeforeRequest = { }
        )
        val result = adapter.scan(listOf(chicken))
        assertTrue(result.complete)
        assertTrue(result.candidates.getValue("chicken").isEmpty())
    }
}
