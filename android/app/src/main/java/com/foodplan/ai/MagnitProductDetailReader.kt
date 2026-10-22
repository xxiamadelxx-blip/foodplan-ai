package com.foodplan.ai

import com.foodplan.domain.RetailProductEvidence
import com.foodplan.domain.ObservedProduct
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Reads only visible copy from an official open Magnit SKU page. No hidden API,
 * headless scraping, CAPTCHA bypass, checkout requests or AI.
 * Merchant disclosures are NOT proof of what a physical pack will contain.
 */
object MagnitProductDetailReader {
    const val SCRIPT = """
        (function () {
          if (location.protocol !== 'https:' || location.hostname !== 'magnit.ru') return JSON.stringify({error:'origin'});
          const lines = (document.body.innerText || '').split(/\n+/).map(s => s.trim()).filter(Boolean);
          function near(label) {
            const i = lines.findIndex(s => s.toLowerCase() === label.toLowerCase());
            if (i < 0 || i + 1 >= lines.length) return '';
            const value = lines[i + 1] || '';
            const headings = ['состав', 'производитель', 'бренд', 'характеристики',
                              'описание', 'отзывы', 'доставка', 'наличие',
                              'пищевая ценность', 'условия хранения'];
            if (headings.includes(value.toLowerCase())) return '';
            return value.length <= 900 ? value : '';
          }
          return JSON.stringify({
            page: location.href,
            manufacturer: near('Производитель') || near('Изготовитель'),
            brand: near('Бренд'),
            composition: near('Состав'),
            characteristics: near('Характеристики'),
            availability: '',
            title: document.querySelector('h1')?.innerText || '',
            supplierAmbiguous: /его поставляют разные производители|бренд\\s+различные бренды/i
                .test(document.body.innerText || '')
          });
        })()
    """

    data class Details(
        val url: String,
        val manufacturer: String?,
        val composition: String?,
        val identitySpec: String?,
        val brand: String?,
        val supplierAmbiguous: Boolean = false
    )

    private val sectionHeadings = setOf(
        "состав", "производитель", "бренд", "характеристики", "описание",
        "отзывы", "доставка", "наличие", "пищевая ценность", "условия хранения"
    )

    private fun fieldOrNull(value: String, maxLength: Int): String? {
        val text = value.trim()
        if (text.isEmpty() || text.length > maxLength ||
            text.lowercase().replace('ё', 'е') in sectionHeadings) return null
        return text
    }

    fun parseResult(jsResult: String?, expectedUrl: String): Details? {
        if (jsResult.isNullOrBlank()) return null
        return try {
            val quoted = JSONTokener(jsResult).nextValue() as? String ?: return null
            val json = JSONObject(quoted)
            val actual = json.optString("page").substringBefore('?').trimEnd('/')
            val expected = expectedUrl.substringBefore('?').trimEnd('/')
            if (!actual.startsWith("https://magnit.ru/product/") || actual != expected) return null
            val manufacturer = fieldOrNull(json.optString("manufacturer"), 180)
            val composition = fieldOrNull(json.optString("composition"), 900)
            val title = json.optString("title").trim().takeIf { it.length in 3..240 }
            val brand = fieldOrNull(json.optString("brand"), 120)
            Details(expectedUrl, manufacturer, composition, title, brand,
                json.optBoolean("supplierAmbiguous", false) ||
                    brand?.lowercase()?.replace('ё', 'е') == "различные бренды")
        } catch (_: Exception) { null }
    }

    /** Merchant text is input evidence only, never allergy or physical-label approval. */
    fun enrich(
        product: ObservedProduct,
        details: Details?,
        storeName: String,
        city: String,
        nowEpochMs: Long,
        userSelectedAddress: Boolean
    ): RetailProductEvidence = RetailProductEvidence(
        product, storeName, city, nowEpochMs, userSelectedAddress,
        manufacturer = details?.manufacturer,
        ingredientsText = details?.composition,
        identitySpec = details?.identitySpec,
        labelSourceUrl = details?.url,
        labelChecked = details != null && details.manufacturer != null &&
            details.composition != null && details.identitySpec != null,
        explicitAllergenReview = false,
        publicCardSupplierAmbiguity = details?.supplierAmbiguous == true
    )
}
