package com.foodplan.ai

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class MagnitPublicSkuPriceReaderTest {
    private val url = "https://magnit.ru/product/1000091750-moloko_vkusnoteevo_ultrapasterizovannoe_3_2_900g"

    @Test fun parserRequiresMatchingCanonicalSkuPositiveKopeksAndExactMass() {
        fun wrap(title: String, price: String, page: String = url) =
            JSONObject.quote(JSONObject().put("page",page)
                .put("title",title).put("priceText",price).toString())
        val real = MagnitPublicSkuPriceReader.parseResult(
            wrap("Молоко Вкуснотеево ультрапастеризованное 3.2% 900г", "114,99 ₽"),
            url
        )
        assertNotNull(real)
        assertEquals(900,real!!.packGrams)
        assertEquals(11499L,real.priceKopeks)
        assertEquals(url,real.productUrl)
        assertNull(MagnitPublicSkuPriceReader.parseResult(
            wrap("Молоко Вкуснотеево 3.2% 900мл","114,99 ₽"), url))
        assertNull(MagnitPublicSkuPriceReader.parseResult(
            wrap("Молоко 900г","114,99 ₽","https://magnit.ru/product/wrong"),url))
        assertNull(MagnitPublicSkuPriceReader.parseResult(
            wrap("Молоко 900г","нет цены"),url))
        assertNull(MagnitPublicSkuPriceReader.parseResult(
            wrap("Молоко 900г","0 ₽"),url))
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Test fun directSkuWebViewNeverBorrowsOldPriceOrRelatedProducts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val done = CountDownLatch(1)
        val raw = AtomicReference<String?>()
        val viewHolder = AtomicReference<WebView>()
        val html = """
            <html><body>
              <h1>Молоко Вкуснотеево ультрапастеризованное 3.2% 900г</h1>
              <del><span>999,99 ₽</span></del>
              <div><span>114,99 ₽</span></div>
              <h2>Похожие</h2>
              <div><span>1,00 ₽</span></div>
            </body></html>
        """.trimIndent()
        instrumentation.runOnMainSync {
            val view = WebView(instrumentation.targetContext)
            viewHolder.set(view)
            view.settings.javaScriptEnabled = true
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(MagnitPublicSkuPriceReader.SCRIPT) {
                        raw.set(it)
                        done.countDown()
                    }
                }
            }
            view.loadDataWithBaseURL(url,html,"text/html","UTF-8",null)
        }
        try {
            assertTrue("WebView direct SKU timeout", done.await(15,TimeUnit.SECONDS))
            val parsed = MagnitPublicSkuPriceReader.parseResult(raw.get(), url)
            assertNotNull(parsed)
            assertEquals(11499L,parsed!!.priceKopeks)
            assertEquals(900,parsed.packGrams)
        } finally {
            instrumentation.runOnMainSync { viewHolder.get()?.destroy() }
        }
    }
}
