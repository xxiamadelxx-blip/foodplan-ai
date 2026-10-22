package com.foodplan.ai

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foodplan.domain.ObservedProduct
import com.foodplan.domain.RetailAuditStatus
import com.foodplan.domain.DeterministicRetailAuditor
import com.foodplan.domain.AnonymousRetailQuery
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MagnitProductDetailReaderTest {
    private val url = "https://magnit.ru/product/123-rice"
    private fun response(page: String = url, manufacturer: String = "Фабрика",
                         composition: String = "Рис шлифованный",
                         title: String = "Рис длиннозерный шлифованный 1кг",
                         multipleSuppliers: Boolean = false) =
        JSONObject.quote(JSONObject().put("page", page)
            .put("manufacturer", manufacturer).put("composition", composition)
            .put("title", title).put("brand", "Условный бренд")
            .put("supplierAmbiguous", multipleSuppliers).toString())

    @Test fun correctSkuYieldsProducerAndIngredientDetails() {
        val details = MagnitProductDetailReader.parseResult(response(), url)
        assertNotNull(details)
        assertEquals("Фабрика", details!!.manufacturer)
        assertEquals("Рис шлифованный", details.composition)
        assertEquals("Рис длиннозерный шлифованный 1кг", details.identitySpec)
        val evidence = MagnitProductDetailReader.enrich(
            ObservedProduct(details.identitySpec!!, url, 1000, 12000L),
            details, "Магнит", "Санкт-Петербург", 10000L, true
        )
        val r = DeterministicRetailAuditor.audit(
            AnonymousRetailQuery("rice", "рис", "plant", 900, listOf("рис")),
            listOf(evidence), "Магнит", "Санкт-Петербург", 10000L
        )
        assertEquals(RetailAuditStatus.COMPARABLE, r.candidates.single().status)
    }

    @Test fun wrongSkuAndMissingCompositionNeverApprove() {
        assertNull(MagnitProductDetailReader.parseResult(
            response(page="https://magnit.ru/product/wrong"),url))
        assertNull(MagnitProductDetailReader.parseResult(
            response(page="https://evil.com/product/123-rice"),url))
        assertNull(MagnitProductDetailReader.parseResult("not valid json",url))
        val details = MagnitProductDetailReader.parseResult(
            response(composition = "", manufacturer = ""),url)
        assertNotNull(details)
        val e=MagnitProductDetailReader.enrich(
            ObservedProduct("Рис длиннозерный шлифованный 1кг",url,1000,12000L),
            details,"Магнит","Санкт-Петербург",10000L,true)
        assertFalse(e.labelChecked)
    }
    @Test fun neighboringSectionHeadingsCannotBecomeFakeProducerOrIngredients() {
        for (heading in listOf("Состав", "Бренд", "Характеристики", "Отзывы", "Доставка")) {
            val details = MagnitProductDetailReader.parseResult(
                response(manufacturer = heading, composition = "Производитель"), url
            )
            assertNotNull(details)
            assertNull("Heading mistaken for manufacturer: $heading", details!!.manufacturer)
            assertNull("Heading mistaken for composition: $heading", details.composition)
            val evidence = MagnitProductDetailReader.enrich(
                ObservedProduct("Рис длиннозерный шлифованный 1кг", url, 1000, 12000L),
                details, "Магнит", "Санкт-Петербург", 10000L, true
            )
            assertFalse("Unverified merchant disclosure must not pass", evidence.labelChecked)
        }
    }

    @Test fun multipleProducerWarningsBlockPriceEquivalence() {
        val details = MagnitProductDetailReader.parseResult(
            response(multipleSuppliers = true), url)
        assertNotNull(details)
        assertTrue(details!!.supplierAmbiguous)
        val evidence = MagnitProductDetailReader.enrich(
            ObservedProduct("Рис длиннозерный шлифованный 1кг", url, 1000, 12000L),
            details, "Магнит", "Санкт-Петербург", 10000L, true)
        assertTrue(evidence.publicCardSupplierAmbiguity)
        val result = DeterministicRetailAuditor.audit(
            AnonymousRetailQuery("rice", "рис", "plant", 900, listOf("рис")),
            listOf(evidence), "Магнит", "Санкт-Петербург", 10000L)
        assertEquals(RetailAuditStatus.REJECTED, result.candidates.single().status)
        assertNull(result.cheapestComparable)
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Test fun realWebViewExecutesVisibleInlineProducerAndCompositionScript() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val completed = CountDownLatch(1)
        val raw = AtomicReference<String?>()
        val web = AtomicReference<WebView>()
        val html = """
            <html><body>
              <h1>Рис длиннозерный шлифованный 1кг</h1>
              <section>
                <div>Состав</div><div>Рис шлифованный</div>
                <div>Характеристики</div>
                <div>Бренд Увелка</div>
                <div>Производитель Ресурс</div>
              </section>
            </body></html>
        """.trimIndent()
        instrumentation.runOnMainSync {
            val view = WebView(instrumentation.targetContext)
            web.set(view)
            view.settings.javaScriptEnabled = true
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                    view?.evaluateJavascript(MagnitProductDetailReader.SCRIPT) { value ->
                        raw.set(value)
                        completed.countDown()
                    }
                }
            }
            view.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
        }
        try {
            assertTrue("Offline WebView did not execute DOM script",
                completed.await(15, TimeUnit.SECONDS))
            val details = MagnitProductDetailReader.parseResult(raw.get(), url)
            assertNotNull("Exact SKU parser must accept the mock official origin", details)
            assertEquals("Ресурс", details!!.manufacturer)
            assertEquals("Увелка", details.brand)
            assertEquals("Рис шлифованный", details.composition)
            assertFalse(details.supplierAmbiguous)
        } finally {
            instrumentation.runOnMainSync { web.get()?.destroy() }
        }
    }

}
