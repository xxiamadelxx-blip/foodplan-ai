package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class TasteSurveyV2Test {
    @Test fun familiarSurveyHas25UniqueDishes() {
        assertEquals(2, TasteSurvey.CATALOG_VERSION)
        assertEquals(25, TasteSurvey.dishes.size)
        assertEquals(25, TasteSurvey.dishes.map { it.id }.distinct().size)
        assertEquals("Овсяная каша", TasteSurvey.dishes.first().title)
        assertFalse(TasteSurvey.dishes.any { it.id in setOf("tom_yum", "ramen", "shrimp_pasta") })
    }

    @Test fun oldVotesSurviveOnlyWhenDishIdentityIsUnchanged() {
        val old = mapOf("borsch" to TasteRating.LIKE,
            "ramen" to TasteRating.DISLIKE,
            "tom_yum" to TasteRating.LIKE,
            "omelette" to TasteRating.OK)
        val migrated = TasteSurvey.migrateVotes(old)
        assertEquals(setOf("borsch", "omelette"), migrated.keys)
        assertEquals(TasteRating.LIKE, migrated["borsch"])
        assertEquals(TasteRating.OK, migrated["omelette"])
        assertEquals(TasteSurvey.tasteWeights(migrated), TasteSurvey.tasteWeights(old))
    }

    @Test fun neverTriedAndSkippedAreNotDislikes() {
        val baseline = TasteSurvey.tasteWeights(emptyMap())
        assertEquals(baseline, TasteSurvey.tasteWeights(mapOf(
            "chicken_curry_v2" to TasteRating.NEVER_TRIED)))
        assertTrue(TasteSurvey.tasteWeights(mapOf(
            "chicken_curry_v2" to TasteRating.DISLIKE))
            .getValue("курица") < 0)
    }
}
