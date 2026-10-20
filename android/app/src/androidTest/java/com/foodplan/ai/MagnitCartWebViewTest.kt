package com.foodplan.ai

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MagnitCartWebViewTest {
    @Test fun officialAddButtonIsClickedOnceInSandboxedProductPage() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val complete=CountDownLatch(1)
        var result: String?=null
        var clickCount:String?=null
        var browser:WebView?=null
        instrumentation.runOnMainSync {
            browser=WebView(instrumentation.targetContext).apply {
                settings.javaScriptEnabled=true
                webViewClient=object:WebViewClient() {
                    override fun onPageFinished(view:WebView?,url:String?) {
                        view?.evaluateJavascript(MagnitCartControl.CLICK_ONCE) { raw ->
                            result=MagnitCartControl.parseJavascriptResult(raw)
                            view.evaluateJavascript("window.__pressed.toString()") { count ->
                                clickCount=count
                                complete.countDown()
                            }
                        }
                    }
                }
                loadDataWithBaseURL(
                    "https://magnit.ru/product/1001-ris_800g",
                    """<html><body><script>window.__pressed=0;</script>
                    <button data-test-id="v-product-detail-add-to-basket-button"
                    onclick="window.__pressed++">Добавить в корзину</button>
                    </body></html>""",
                    "text/html","UTF-8",null
                )
            }
        }
        try {
            assertTrue("WebView click simulation timed out",complete.await(30,TimeUnit.SECONDS))
            assertEquals("CLICK_ATTEMPTED_NOT_VERIFIED",result)
            assertEquals("\"1\"",clickCount)
        } finally {
            instrumentation.runOnMainSync { browser?.destroy() }
        }
    }
}
