package com.foodplan.ai

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
    }

    @Test fun unauthorizedOriginAndEmptyResponseProduceNoQuotes() {
        assertTrue(MagnitDomReader.parseResult(
            response("https://another-store.ru/catalog/64121","89,99 ₽")).isEmpty())
        assertTrue(MagnitDomReader.parseResult("null").isEmpty())
        assertTrue(MagnitDomReader.parseResult("not json").isEmpty())
    }
}
