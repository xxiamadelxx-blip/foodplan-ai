package com.foodplan.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foodplan.domain.CookingEffort
import com.foodplan.domain.FoodProfile
import com.foodplan.domain.KitchenEquipment
import com.foodplan.domain.PlanningResult
import com.foodplan.domain.RecipeBatchPlanner
import com.foodplan.domain.SavedWeekPlanner
import com.foodplan.domain.SevenDayPlanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The full Android UI suite never modifies this fixture: the normal run
 * verifies DataStore availability only. An explicit host-side ADB probe
 * invokes this SAME test with -e foodplan_m5_seed 1, then terminates the app
 * from OUTSIDE the instrumentation process and performs a cold relaunch.
 */
@RunWith(AndroidJUnit4::class)
class M5ColdRestartSeedTest {
    @Test fun prepareSavedWeekForExternalProcessRestart() = runBlocking {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ProfileStore(targetContext)
        val args = InstrumentationRegistry.getArguments()
        if (args.getString("foodplan_m5_seed") != "1") {
            assertNotNull("DataStore snapshot must be accessible", store.snapshot.first().profile)
            return@runBlocking
        }

        // Synthetic test-only profile, never a user's personal data.
        val fixture = FoodProfile(
            sex = "Мужской", age = "30", heightCm = "175", weightKg = "75",
            goal = "Поддержание", budgetRub = "5000", city = "Санкт-Петербург",
            stores = setOf("Пятёрочка"), household = "2", mealsPerDay = "3",
            cookingMinutes = "120", cookingEffort = CookingEffort.ALL_OUT,
            appliances = KitchenEquipment.choices.toSet(),
            allergens = "", disliked = "", diet = "Всеядный"
        )
        store.save(fixture)
        store.clearWeekSelection()

        // Use precisely the same persisted vote/pantry keys as MainActivity.
        // Earlier tests may have written fixtures to these maps.
        val first = store.snapshot.first()
        val baseline = SevenDayPlanner.plan(first.profile, first.tasteVotes,
            first.pantryGrams) as PlanningResult.Success
        val chosenRecipeId = baseline.days[0].meals[0].recipe.id
        val batched = RecipeBatchPlanner.apply(first.profile, first.tasteVotes,
            baseline, 0, 0, chosenRecipeId, 3, CookingEffort.ALL_OUT,
            first.pantryGrams) as PlanningResult.Success
        val selection = SavedWeekPlanner.capture(batched, first.profile,
            first.tasteVotes, first.pantryGrams)
        store.saveWeekSelection(selection)

        val second = ProfileStore(targetContext).snapshot.first()
        assertTrue(second.previouslySaved)
        assertEquals(selection, second.savedWeekSelection)
        val restored = SavedWeekPlanner.restore(second.profile, second.tasteVotes,
            second.pantryGrams,
            SevenDayPlanner.plan(second.profile, second.tasteVotes, second.pantryGrams),
            second.savedWeekSelection) as PlanningResult.Success
        assertEquals(3, restored.days[0].meals[0].batchDays)
        assertEquals(0, restored.days[0].meals[0].batchStartDay)
        assertEquals(3, restored.days[1].meals[0].batchDays)
    }
}
