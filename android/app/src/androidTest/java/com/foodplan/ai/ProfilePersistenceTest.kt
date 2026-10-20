package com.foodplan.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foodplan.domain.FoodProfile
import com.foodplan.domain.CookingEffort
import com.foodplan.domain.TasteRating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a real Android runtime, validating DataStore durability across store instances. */
@RunWith(AndroidJUnit4::class)
class ProfilePersistenceTest {
    @Test
    fun dataStoreRoundTripWithNewRepositoryInstance() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expected = FoodProfile(
            sex = "Женский", age = "29", heightCm = "165", weightKg = "72,5",
            activity = "Высокая", goal = "Поддержание",
            city = "Санкт-Петербург", budgetRub = "4500",
            stores = setOf("Пятёрочка", "Лента"), diet = "Пескетарианский",
            allergens = "арахис, молоко", disliked = "сельдерей",
            household = "2", mealsPerDay = "4", cookingMinutes = "30",
            cookingEffort = CookingEffort.ALL_OUT,
            appliances = setOf("Плита", "Мультиварка", "Аэрогриль", "Сковорода")
        )
        ProfileStore(context).save(expected)
        val loaded = ProfileStore(context).snapshot.first()
        assertTrue(loaded.previouslySaved)
        assertEquals(expected, loaded.profile)
    }
    @Test
    fun tasteVoteCanBeReplacedAndPersisted() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ProfileStore(context).saveTasteVote("ramen", TasteRating.DISLIKE)
        ProfileStore(context).saveTasteVote("ramen", TasteRating.LIKE)
        val fromNewStore = ProfileStore(context).snapshot.first()
        assertEquals(TasteRating.LIKE, fromNewStore.tasteVotes["ramen"])
    }

    @Test fun manualPriceStaysScopedToCityAndStore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = ProfileStore(context)
        storage.saveManualPrice("Санкт-Петербург", "Лента", "rice", 12999L)
        val snapshot = ProfileStore(context).snapshot.first()
        assertEquals(12999L, snapshot.manualPrices["Санкт-Петербург|Лента|rice"])
        assertEquals(null, snapshot.manualPrices["Москва|Лента|rice"])
    }

}
