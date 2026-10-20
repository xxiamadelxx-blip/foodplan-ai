package com.foodplan.domain

data class RecipeSafety(
    val ingredientIds: Set<String>,
    val allergenIds: Set<String>,
    val completeIngredientAndAllergenData: Boolean
)
enum class Eligibility { ALLOWED, BLOCKED_ALLERGEN, BLOCKED_INGREDIENT, UNKNOWN_COMPOSITION }
object RecipeEligibility {
    fun check(recipe: RecipeSafety, allergens: Set<String>, excluded: Set<String>): Eligibility {
        if (recipe.ingredientIds.any { it in excluded }) return Eligibility.BLOCKED_INGREDIENT
        if (recipe.allergenIds.any { it in allergens }) return Eligibility.BLOCKED_ALLERGEN
        if (!recipe.completeIngredientAndAllergenData) return Eligibility.UNKNOWN_COMPOSITION
        return Eligibility.ALLOWED
    }
}
