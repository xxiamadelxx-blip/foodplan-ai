package com.foodplan.ai

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
                         title: String = "Рис длиннозерный шлифованный 1кг") =
        JSONObject.quote(JSONObject().put("page", page)
            .put("manufacturer", manufacturer).put("composition", composition)
            .put("title", title).put("brand", "Условный бренд").toString())

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
}
