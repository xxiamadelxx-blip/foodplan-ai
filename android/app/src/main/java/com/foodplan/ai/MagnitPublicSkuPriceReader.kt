package com.foodplan.ai

import com.foodplan.domain.CatalogValueParser
import com.foodplan.domain.MagnitPackOnlySkuCatalog
import com.foodplan.domain.ListingAvailability
import com.foodplan.domain.ObservedProduct
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Reads a visible PRICE from a public, user-initiated official product page.
 * This can be used when the corresponding catalogue section has no stable
 * price card. A quote is not a checkout price, physical label or stock check.
 * The caller MUST still verify ingredient identity and price freshness.
 */
internal object MagnitPublicSkuPriceReader {
    const val SCRIPT = """
        (function () {
          if (location.protocol !== 'https:' || location.hostname !== 'magnit.ru' ||
              !/^\/product\/[a-z0-9_-]+\/?$/i.test(location.pathname))
            return JSON.stringify({error:'origin'});
          const h1 = document.querySelector('h1');
          if (!h1 || h1.getClientRects().length === 0)
            return JSON.stringify({error:'title'});
          const title = (h1.innerText || '').trim();
          const pricePattern = /^\s*\d{1,7}(?:[,.]\d{1,2})?\s*₽\s*$/;
          const follows = (a,b) => Boolean(a.compareDocumentPosition(b) & 4);
          const stop = Array.from(document.querySelectorAll('h2,h3'))
              .find(el => follows(h1, el) &&
                  /^(похожие|рекомендуем|с этим товаром покупают)$/i
                    .test((el.innerText || '').trim()));
          const isVisible = el => {
            if (!el || el.getClientRects().length === 0) return false;
            if (el.closest('del,s,strike,[class*="old-price"],[class*="price-old"]'))
              return false;
            for (let node=el; node && node.nodeType === 1; node=node.parentElement) {
              const css = getComputedStyle(node);
              if (css.display === 'none' || css.visibility === 'hidden' ||
                  css.textDecorationLine.includes('line-through')) return false;
            }
            return true;
          };
          // Only exact visible price elements positioned AFTER the H1, BEFORE
          // "Похожие". Never borrow prices from related cards or hidden state.
          const priceElements = Array.from(document.querySelectorAll('span,strong,div,p'))
              .filter(el => isVisible(el) && follows(h1,el) &&
                   (!stop || follows(el,stop)) &&
                   pricePattern.test((el.innerText || '').trim()))
              .sort((a,b) => follows(a,b) ? -1 : (follows(b,a) ? 1 : 0));
          return JSON.stringify({
            page: location.origin + location.pathname,
            title: title.slice(0,240),
            priceText: priceElements.length ? priceElements[0].innerText.trim() : ''
          });
        })()
    """

    private val official = Regex("""^https://magnit\.ru/product/[A-Za-z0-9_-]+/?$""")

    fun parseResult(value: String?, exactUrl: String): ObservedProduct? {
        if (value.isNullOrBlank() || !official.matches(exactUrl)) return null
        return try {
            val raw = JSONTokener(value).nextValue() as? String ?: return null
            val item = JSONObject(raw)
            val page = item.optString("page")
            if (page != exactUrl.trimEnd('/')) return null
            val name = item.optString("title").trim()
            if (name.length !in 5..240) return null
            val registered = MagnitPackOnlySkuCatalog.forUrl(exactUrl)
            val grams = if (registered == null)
                CatalogValueParser.packageGrams(name) ?: return null
            else null
            val price = CatalogValueParser.parseDisplayedRubles(
                item.optString("priceText")
            ) ?: return null
            if (grams != null && grams !in 1..50000) return null
            if (price !in 1L..10_000_000L) return null
            val product = ObservedProduct(name, exactUrl, grams, price,
                ListingAvailability.UNKNOWN, purchasePack = registered?.pack)
            // A carton/bottle can publish a visible price without proving
            // a conversion into edible grams. The on-page title MUST show
            // the exact expected piece count or millilitre volume.
            if (registered != null &&
                MagnitPackOnlySkuCatalog.matching(product) != registered) return null
            product
        } catch (_: Exception) { null }
    }
}
