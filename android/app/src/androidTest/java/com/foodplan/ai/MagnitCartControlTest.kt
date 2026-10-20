package com.foodplan.ai

import org.junit.Assert.*
import org.junit.Test

class MagnitCartControlTest {
    @Test fun parsesOnlyExpectedSignals() {
        assertEquals("CLICK_ATTEMPTED_NOT_VERIFIED",
            MagnitCartControl.parseJavascriptResult("\"CLICK_ATTEMPTED_NOT_VERIFIED\""))
        assertEquals("WRONG_PAGE",
            MagnitCartControl.parseJavascriptResult("\"WRONG_PAGE\""))
        assertEquals("NO_AVAILABLE_ADD_BUTTON",
            MagnitCartControl.parseJavascriptResult("\"NO_AVAILABLE_ADD_BUTTON\""))
        assertEquals("INVALID_RESULT",MagnitCartControl.parseJavascriptResult("undefined"))
    }

    @Test fun scriptRestrictedToProductPageAndDoesNotAutoCheckout() {
        assertTrue(MagnitCartControl.CLICK_ONCE.contains("location.hostname !== 'magnit.ru'"))
        assertTrue(MagnitCartControl.CLICK_ONCE.contains("/product/"))
        assertFalse(MagnitCartControl.CLICK_ONCE.contains("оформить заказ"))
    }
}
