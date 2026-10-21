package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class SavedWeekPlannerTest {
    private fun profile() = FoodProfile(
        sex="Мужской",age="30",heightCm="175",weightKg="75",
        goal="Поддержание",budgetRub="5000",city="Санкт-Петербург",
        stores=setOf("Магнит"),household="2",mealsPerDay="3",
        cookingMinutes="120",cookingEffort=CookingEffort.ALL_OUT,
        appliances=KitchenEquipment.choices.toSet()
    )
    private fun base(p: FoodProfile) =
        SevenDayPlanner.plan(p) as PlanningResult.Success

    @Test fun threeDayRecipeAndFullShoppingRestoreAfterNewPlannerInstance() {
        val p=profile()
        val first=base(p)
        val savedRecipe=first.days[0].meals[0].recipe.id
        val revised=RecipeBatchPlanner.apply(p,emptyMap(),first,0,0,
            savedRecipe,3,CookingEffort.ALL_OUT) as PlanningResult.Success
        val saved=SavedWeekPlanner.capture(revised,p,emptyMap(),emptyMap())
        val freshBaseline=base(p)
        val restored=SavedWeekPlanner.restore(p,emptyMap(),emptyMap(),
            freshBaseline,saved) as PlanningResult.Success

        assertEquals(revised.days,restored.days)
        assertEquals(revised.shopping,restored.shopping)
        assertEquals(ShoppingAggregation.fromDays(restored.days,2),restored.shopping)
        assertEquals(3,restored.days[0].meals[0].batchDays)
        assertEquals(0,restored.days[2].meals[0].batchStartDay)
    }

    @Test fun replacementRecipeStaysAfterRestore() {
        val p=profile()
        val original=base(p)
        val alt=MealReplacement.alternatives(p,emptyMap(),original,1,1).first {
            it.id != original.days[1].meals[1].recipe.id
        }
        val changed=RecipeBatchPlanner.apply(p,emptyMap(),original,1,1,
            alt.id,1,CookingEffort.ALL_OUT) as PlanningResult.Success
        val saved=SavedWeekPlanner.capture(changed,p,emptyMap(),emptyMap())
        val restored=SavedWeekPlanner.restore(p,emptyMap(),emptyMap(),base(p),saved)
            as PlanningResult.Success
        assertEquals(alt.id,restored.days[1].meals[1].recipe.id)
        assertEquals(changed.shopping,restored.shopping)
    }

    @Test fun storeCityOrExactBudgetChangesDoNotDestroyChosenMenu() {
        val p=profile()
        val initial=base(p)
        val changed=RecipeBatchPlanner.apply(p,emptyMap(),initial,0,0,
            initial.days[0].meals[0].recipe.id,3,CookingEffort.ALL_OUT)
                as PlanningResult.Success
        val saved=SavedWeekPlanner.capture(changed,p,emptyMap(),emptyMap())
        val shifted=p.copy(city="Москва",stores=setOf("Пятёрочка"),budgetRub="356")
        assertEquals(
            SavedWeekPlanner.profileFingerprint(p,emptyMap(),emptyMap()),
            SavedWeekPlanner.profileFingerprint(shifted,emptyMap(),emptyMap()))
        val result=SavedWeekPlanner.restore(shifted,emptyMap(),emptyMap(),
            base(shifted),saved) as PlanningResult.Success
        assertEquals(3,result.days[0].meals[0].batchDays)
        assertEquals(changed.shopping,result.shopping)
    }

    @Test fun householdOrDietChangesInvalidateSelectionsAndRecalculate() {
        val p=profile()
        val baseline=base(p)
        val changed=RecipeBatchPlanner.apply(p,emptyMap(),baseline,0,0,
            baseline.days[0].meals[0].recipe.id,2,CookingEffort.ALL_OUT)
                as PlanningResult.Success
        val saved=SavedWeekPlanner.capture(changed,p,emptyMap(),emptyMap())
        val newProfile=p.copy(household="4")
        val newBase=base(newProfile)
        assertEquals(newBase,SavedWeekPlanner.restore(
            newProfile,emptyMap(),emptyMap(),newBase,saved))
        assertNotEquals(newBase.shopping,changed.shopping)
    }

    @Test fun differentPantryOrTasteVotesInvalidateSelections() {
        val p=profile()
        val saved=SavedWeekPlanner.capture(base(p),p,emptyMap(),emptyMap())
        val pantry=mapOf("rice" to 500)
        val pantryBase=SevenDayPlanner.plan(p,emptyMap(),pantry)
        assertEquals(pantryBase,SavedWeekPlanner.restore(
            p,emptyMap(),pantry,pantryBase,saved))
        val votes=mapOf("ramen" to TasteRating.DISLIKE)
        val voteBase=SevenDayPlanner.plan(p,votes)
        assertEquals(voteBase,SavedWeekPlanner.restore(
            p,votes,emptyMap(),voteBase,saved))
    }

    @Test fun invalidRecipeIdCannotSneakIntoAnExistingWeek() {
        val p=profile()
        val initial=base(p)
        val saved=SavedWeekPlanner.capture(initial,p,emptyMap(),emptyMap())
        val poisonedDay=saved.mealsByDay[0].toMutableList()
        poisonedDay[1]=poisonedDay[1].copy(recipeId="not_in_recipe_catalog")
        val rows=saved.mealsByDay.toMutableList()
        rows[0]=poisonedDay
        assertEquals(initial,SavedWeekPlanner.restore(p,emptyMap(),emptyMap(),
            initial,saved.copy(mealsByDay=rows)))
    }

    @Test fun corruptedBatchSpanAndScalingFailClosed() {
        val p=profile()
        val initial=base(p)
        val updated=RecipeBatchPlanner.apply(p,emptyMap(),initial,0,0,
            initial.days[0].meals[0].recipe.id,3,CookingEffort.ALL_OUT)
                as PlanningResult.Success
        val saved=SavedWeekPlanner.capture(updated,p,emptyMap(),emptyMap())
        val rows=saved.mealsByDay.toMutableList()
        rows[2]=rows[2].toMutableList().also {
            it[0]=it[0].copy(recipeId="fake",servings=200.0)
        }
        assertEquals(initial,SavedWeekPlanner.restore(p,emptyMap(),emptyMap(),
            initial,saved.copy(mealsByDay=rows)))
        val orphan=saved.mealsByDay.toMutableList()
        orphan[1]=orphan[1].toMutableList().also {
            it[0]=it[0].copy(batchStartDay=null)
        }
        assertEquals(initial,SavedWeekPlanner.restore(p,emptyMap(),emptyMap(),
            initial,saved.copy(mealsByDay=orphan)))
    }

    @Test fun blockedMedicalRestrictionsCannotBeBypassedWithOldSavedPlan() {
        val p=profile()
        val saved=SavedWeekPlanner.capture(base(p),p,emptyMap(),emptyMap())
        val allergic=p.copy(allergens="арахис")
        val blocked=SevenDayPlanner.plan(allergic)
        assertTrue(blocked is PlanningResult.Blocked)
        assertEquals(blocked,SavedWeekPlanner.restore(allergic,emptyMap(),
            emptyMap(),blocked,saved))
    }

    @Test fun applianceSetOrderAcrossRestartsHasSameFingerprint() {
        val p=profile()
        val toolList=p.appliances.toList()
        val same=p.copy(appliances=toolList.reversed().toCollection(linkedSetOf()))
        assertEquals(SavedWeekPlanner.profileFingerprint(p,emptyMap(),emptyMap()),
            SavedWeekPlanner.profileFingerprint(same,emptyMap(),emptyMap()))
    }

    @Test fun changingOrderOfMapsDoesNotChangeProfileFingerprint() {
        val p=profile()
        val a=SavedWeekPlanner.profileFingerprint(p,
            linkedMapOf("ramen" to TasteRating.LIKE,"borsch" to TasteRating.OK),
            linkedMapOf("rice" to 500,"chicken" to 300))
        val b=SavedWeekPlanner.profileFingerprint(p,
            linkedMapOf("borsch" to TasteRating.OK,"ramen" to TasteRating.LIKE),
            linkedMapOf("chicken" to 300,"rice" to 500))
        assertEquals(a,b)
        assertEquals(64,a.length)
    }
}
