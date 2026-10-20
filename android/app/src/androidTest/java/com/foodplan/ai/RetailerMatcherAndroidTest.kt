package com.foodplan.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foodplan.domain.RetailerProductMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android uses a different java.util.regex implementation than desktop JVM.
 * Exercise the Russian processed-food guard on the emulator, not just unit tests.
 */
@RunWith(AndroidJUnit4::class)
class RetailerMatcherAndroidTest {
    @Test fun unicodeRussianFoodStatesMustWorkOnAndroidRegexEngine() {
        assertFalse(RetailerProductMatcher.matchesIngredient("rice", "Рис вареный 800г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("potato", "Картофель фри 1кг"))
        assertFalse(RetailerProductMatcher.matchesIngredient("rice", "Рис с овощами 800г"))
        assertFalse(RetailerProductMatcher.matchesIngredient("pasta", "Паста томатная 500г"))
        assertTrue(RetailerProductMatcher.matchesIngredient("rice", "Рис длиннозерный сухой 800г"))
        assertTrue(RetailerProductMatcher.matchesIngredient("potato", "Картофель свежий 2кг"))
    }
}
