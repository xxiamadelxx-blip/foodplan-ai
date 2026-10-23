package com.foodplan.ai

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.foodplan.domain.*

/**
 * Actual IN-APP automatic public catalogue reading, not a GitHub-only parser.
 * The official WebView loads previously observed public catalogue paths,
 * reads visible rendered product prices, then persists comparable mass SKUs
 * for the ordinary Products screen. No AI or manual per-product interaction.
 *
 * The noninteractive WebView stays attached to the activity because a site
 * may require a browser renderer for public JS-generated price labels.
 * No JS interface, cross-host navigation, checkout, cookies export or
 * synthetic fallback prices. An unavailable source simply leaves UNKNOWN.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun MagnitAutomaticPriceUpdater(
    shopping: List<ShoppingNeed>,
    city: String,
    budgetRub: Long,
    strictAllergenVerification: Boolean,
    onProgress: (String) -> Unit,
    onObserved: (RetailWeekQuote, List<String>) -> Unit,
    onMassOffers: (List<RetailerOffer>) -> Unit
) {
    val context = LocalContext.current
    val queries = remember(shopping) { AnonymousRetailPlanner.build(shopping) }
    val latestProgress = rememberUpdatedState(onProgress)
    val latestObserved = rememberUpdatedState(onObserved)
    val latestMassOffers = rememberUpdatedState(onMassOffers)
    val browser = remember(context, queries, city, budgetRub, strictAllergenVerification) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture = true
        }
    }
    val scan = remember(browser, queries, city, budgetRub, strictAllergenVerification) {
        MagnitScanCoordinator(
            browser, queries, city,
            onProgress = { latestProgress.value(it) },
            onCompleted = { evidence, failures ->
                val now = System.currentTimeMillis()
                val quote = RetailWeekQuoter.build(
                    queries, evidence, "Магнит", city, budgetRub, now,
                    strictAllergenVerification)
                latestObserved.value(quote, failures)
                if (!strictAllergenVerification) {
                    val offers = AutomaticRetailScanPolicy.verifiedMassOffers(
                        shopping, evidence, city, budgetRub, now)
                    if (offers.isNotEmpty()) latestMassOffers.value(offers)
                }
            }
        ).also { coordinator ->
            browser.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return true
                    // No redirected tracker, unrelated hostname or checkout.
                    return uri.scheme != "https" || uri.host != "magnit.ru" ||
                        uri.path !in listOf("/", "/robots.txt") &&
                        !uri.path.orEmpty().startsWith("/product/") &&
                        !uri.path.orEmpty().startsWith("/catalog/")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    coordinator.onPageFinished(url)
                }

                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true)
                        coordinator.onMainFrameError(
                            request.url.toString(), "ошибка сети")
                }

                override fun onReceivedHttpError(
                    view: WebView?, request: WebResourceRequest?,
                    response: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true &&
                        (response?.statusCode ?: 0) >= 400)
                        coordinator.onMainFrameError(
                            request.url.toString(), "HTTP ${response?.statusCode}")
                }
            }
        }
    }

    AndroidView(
        factory = { browser },
        modifier = Modifier.fillMaxWidth().height(1.dp).alpha(0f)
            .semantics { invisibleToUser() }
    )
    LaunchedEffect(scan) { scan.begin() }
    DisposableEffect(scan) {
        onDispose {
            scan.cancel()
            browser.stopLoading()
            browser.destroy()
        }
    }
}
