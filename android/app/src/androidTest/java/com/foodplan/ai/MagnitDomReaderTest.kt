package com.foodplan.ai

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MagnitDomReaderTest {
    private fun response(page: String, price: String): String {
        val item = JSONObject().put("title","Сода пищевая 500г в ассортименте")
            .put("url","https://magnit.ru/product/3132360213-soda_pishchevaya_500g")
            .put("priceText",price)
            .put("ratingText","4.8")
            .put("reviewsText","· 1906 отзывов")
        val payload=JSONObject().put("page",page)
            .put("items",JSONArray().put(item)).toString()
        return JSONObject.quote(payload)
    }

    @Test fun observedProductCardHasPageSkuPackAndExactKopeks() {
        val offers=MagnitDomReader.parseResult(
            response("https://magnit.ru/catalog/64121","89,99 ₽"))
        assertEquals(1,offers.size)
        assertEquals("Сода пищевая 500г в ассортименте",offers.single().productName)
        assertEquals(500,offers.single().packGrams)
        assertEquals(8999L,offers.single().priceKopeks)
        assertEquals(4.8,offers.single().ratingStars ?: -1.0,0.001)
        assertEquals(1906,offers.single().reviewCount)
    }

    @Test fun unauthorizedOriginAndEmptyResponseProduceNoQuotes() {
        assertTrue(MagnitDomReader.parseResult(
            response("https://another-store.ru/catalog/64121","89,99 ₽")).isEmpty())
        assertTrue(MagnitDomReader.parseResult("null").isEmpty())
        assertTrue(MagnitDomReader.parseResult("not json").isEmpty())
    }
    @Test fun explicitPublicCataloguePackMassAndSpoofedHost() {
        val actual = JSONObject()
            .put("title", "Картофель")
            .put("url", "https://magnit.ru/product/9072651210-kartofel_1kg")
            .put("priceText", "71,98 ₽")
            .put("packText", "2кг")
        val malicious = JSONObject()
            .put("title", "Картофель")
            .put("url", "https://magnit.ru.evil.test/product/123")
            .put("priceText", "1,00 ₽")
            .put("packText", "5кг")
        val doc = JSONObject()
            .put("page", "https://magnit.ru/catalog/63905-testmmovoshchi_i_frukty")
            .put("items", JSONArray().put(actual).put(malicious))
        val rows = MagnitDomReader.parseResult(JSONObject.quote(doc.toString()))
        assertEquals(1, rows.size)
        assertEquals("Картофель 2кг", rows.single().productName)
        assertEquals(2000, rows.single().packGrams)
        assertEquals(7198L, rows.single().priceKopeks)
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Test fun publicPriceComesFromSameVisibleSkuCardNotCrossedOutPrice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val done = CountDownLatch(1)
        val js = AtomicReference<String?>()
        val web = AtomicReference<WebView>()
        val html = """
            <html><body>
              <article data-testid="product-card">
                <a href="https://magnit.ru/product/3152910005-ris_dlinnozernyy_2_sort_800g_6">
                  <h3>Рис длиннозерный 800г</h3>
                </a>
                <del>129,99 ₽</del>
                <span data-testid="product-price">49,99 ₽</span>
              </article>
              <article data-testid="product-card">
                <a href="https://magnit.ru/product/100-potato">Картофель 2кг</a>
                <span data-testid="product-price">100,25 ₽</span>
              </article>
              <div data-testid="product-card" style="display:none">
                <a href="https://magnit.ru/product/100-fake">Фейк 3кг</a>
                <span data-testid="product-price">1 ₽</span>
              </div>
            </body></html>
        """.trimIndent()
        instrumentation.runOnMainSync {
            val view = WebView(instrumentation.targetContext)
            web.set(view)
            view.settings.javaScriptEnabled = true
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(MagnitDomReader.SCRIPT) { value ->
                        js.set(value)
                        done.countDown()
                    }
                }
            }
            view.loadDataWithBaseURL("https://magnit.ru/catalog/64121",
                html, "text/html", "UTF-8", null)
        }
        try {
            assertTrue("Offline Magnit WebView DOM test timed out",
                done.await(15, TimeUnit.SECONDS))
            val result = MagnitDomReader.parseResult(js.get())
            assertEquals(2, result.size)
            val rice = result.single { it.productName == "Рис длиннозерный 800г" }
            assertEquals(800, rice.packGrams)
            assertEquals(4999L, rice.priceKopeks)
            assertEquals(10025L, result.single {
                it.productName == "Картофель 2кг"
            }.priceKopeks)
        } finally {
            instrumentation.runOnMainSync { web.get()?.destroy() }
        }
    }


    @Test fun registeredGrossSkuInCatalogCannotBecomeEdibleGramPackage() {
        val banana = JSONObject().put("title", "Бананы 1кг")
            .put("url", "https://magnit.ru/product/9072651501-banany")
            .put("priceText", "149,99 ₽")
        val chickpeas = JSONObject().put("title", "Нут Green Ribbon 400г")
            .put("url", "https://magnit.ru/product/8000013399-green_ribbon_nut_400g_zh_b_s_kl")
            .put("priceText", "59,99 ₽")
        val doc = JSONObject().put("page", "https://magnit.ru/catalog/64121")
            .put("items", JSONArray().put(banana).put(chickpeas))
        val observed = MagnitDomReader.parseResult(JSONObject.quote(doc.toString()))
        assertEquals(2, observed.size)
        val fruit = observed.first { "бананы" in it.productName.lowercase() }
        assertNull(fruit.packGrams)
        assertEquals(com.foodplan.domain.PurchaseUnit.UNPEELED_GRAM,
            fruit.purchasePack?.unit)
        assertEquals(1000, fruit.purchasePack?.quantity)
        val can = observed.first { it.productName.startsWith("Нут") }
        assertNull(can.packGrams)
        assertEquals(com.foodplan.domain.PurchaseUnit.NET_WITH_LIQUID_GRAM,
            can.purchasePack?.unit)

        // A mismatched gross weight at the same trusted product URL is rejected,
        // not silently treated as 2kg/200g of edible ingredients.
        val wrong = JSONObject().put("page", "https://magnit.ru/catalog/64121")
            .put("items", JSONArray().put(
                JSONObject().put("title", "Бананы 2кг")
                    .put("url", "https://magnit.ru/product/9072651501-banany")
                    .put("priceText", "149,99 ₽")))
        assertTrue(MagnitDomReader.parseResult(JSONObject.quote(wrong.toString()))
            .isEmpty())
    }
}
