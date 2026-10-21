package com.foodplan.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foodplan.domain.FoodProfile
import com.foodplan.domain.CookingEffort
import com.foodplan.domain.TasteRating
import com.foodplan.domain.SavedWeekPlanner
import com.foodplan.domain.RecipeBatchPlanner
import com.foodplan.domain.PlanningResult
import com.foodplan.domain.SevenDayPlanner
import com.foodplan.domain.ShoppingAggregation
import com.foodplan.domain.KitchenEquipment
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

    @Test fun pantryGramsSurviveDataStoreReopening() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        ProfileStore(context).savePantryGrams("rice",750)
        assertEquals(750,ProfileStore(context).snapshot.first().pantryGrams["rice"])
    }

    @Test fun savedWebSitePriceRemainsAnnotatedWithSkuAndRegion() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scanned = com.foodplan.domain.RetailerOffer(
            "rice","Магнит","Санкт-Петербург","Рис 800г",
            "https://magnit.ru/product/100-rice",800,16999L,
            System.currentTimeMillis(),true
        )
        ProfileStore(context).saveRetailerOffer(scanned)
        val read = ProfileStore(context).snapshot.first()
        assertTrue(read.retailerOffers.any { it == scanned })
        assertTrue(read.retailerOffers.none {
            it.ingredientId=="rice" && it.storeName=="Магнит" && it.city=="Москва"
        })
    }

    @Test fun observedReviewEvidenceAndUncertainStockAreStoredWithoutFakingAvailability() =
        runBlocking {
            val context=InstrumentationRegistry.getInstrumentation().targetContext
            val offer=com.foodplan.domain.RetailerOffer(
                "rice","Магнит","Санкт-Петербург","Рис 800г",
                "https://magnit.ru/product/price-rating-test-rice",
                800,14999L,System.currentTimeMillis(),true,
                com.foodplan.domain.ListingAvailability.UNKNOWN,4.8,200
            )
            ProfileStore(context).saveRetailerOffer(offer)
            val stored=ProfileStore(context).snapshot.first()
                .retailerOffers.first { it.productUrl==offer.productUrl }
            assertEquals(offer,stored)
            assertEquals(com.foodplan.domain.ListingAvailability.UNKNOWN,stored.availability)
        }

    @Test fun clearingNewWeekDeletesNoPersonalDataOrStoreEvidence() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ProfileStore(context)
        val p = FoodProfile(
            sex="Мужской",age="30",heightCm="175",weightKg="75",
            city="Санкт-Петербург",budgetRub="63218",
            stores=setOf("Магнит"),household="2",mealsPerDay="3",
            cookingMinutes="90",cookingEffort=CookingEffort.ALL_OUT,
            appliances=KitchenEquipment.choices.toSet()
        )
        store.save(p)
        store.saveTasteVote("borsch",TasteRating.LIKE)
        store.savePantryGrams("rice",250)
        store.saveManualPrice("Санкт-Петербург","Магнит","rice",14900L)
        val original = ProfileStore(context).snapshot.first()
        val plan = SevenDayPlanner.plan(
            original.profile,original.tasteVotes,original.pantryGrams
        ) as PlanningResult.Success
        val selected = SavedWeekPlanner.capture(
            plan,original.profile,original.tasteVotes,original.pantryGrams)
        store.saveWeekSelection(selected)
        assertEquals(selected,ProfileStore(context).snapshot.first().savedWeekSelection)

        ProfileStore(context).clearWeekSelection()
        val after = ProfileStore(context).snapshot.first()
        assertEquals(null,after.savedWeekSelection)
        assertEquals(original,after)
        assertTrue(after.previouslySaved)
        assertEquals("63218",after.profile.budgetRub)
        assertEquals(TasteRating.LIKE,after.tasteVotes["borsch"])
        assertEquals(250,after.pantryGrams["rice"])
        assertEquals(14900L,after.manualPrices["Санкт-Петербург|Магнит|rice"])
    }

    @Test fun editedWeekSurvivesFreshDataStoreInstanceAndRebuildsShopping() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val p = FoodProfile(
            sex="Мужской", age="30", heightCm="175", weightKg="75",
            goal="Поддержание", budgetRub="5000", city="Санкт-Петербург",
            household="2", mealsPerDay="3", cookingMinutes="120",
            cookingEffort=CookingEffort.ALL_OUT, appliances=KitchenEquipment.choices.toSet()
        )
        val initial = SevenDayPlanner.plan(p) as PlanningResult.Success
        val recipeId = initial.days[0].meals[0].recipe.id
        val edited = RecipeBatchPlanner.apply(p, emptyMap(), initial,
            0, 0, recipeId, 3, CookingEffort.ALL_OUT) as PlanningResult.Success
        val storedSelection = SavedWeekPlanner.capture(edited, p, emptyMap(), emptyMap())

        ProfileStore(context).saveWeekSelection(storedSelection)
        val reloadedSelection = ProfileStore(context).snapshot.first().savedWeekSelection
        assertEquals(storedSelection,reloadedSelection)
        val restored = SavedWeekPlanner.restore(p,emptyMap(),emptyMap(),
            SevenDayPlanner.plan(p),reloadedSelection) as PlanningResult.Success
        assertEquals(edited.days,restored.days)
        assertEquals(ShoppingAggregation.fromDays(restored.days,2),restored.shopping)
        assertEquals(CookingEffort.ALL_OUT,restored.days[0].meals[0].selectedEffort)

        // A new session explicitly starts afresh rather than retaining stale meals.
        ProfileStore(context).clearWeekSelection()
        assertEquals(null,ProfileStore(context).snapshot.first().savedWeekSelection)
    }

}
