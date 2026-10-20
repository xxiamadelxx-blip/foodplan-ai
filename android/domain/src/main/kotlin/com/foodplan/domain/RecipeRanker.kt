package com.foodplan.domain

/**
 * Screening and ranking candidates before future week-planning.
 * No candidate is marked medically safe: free-text allergy normalization and label traceability
 * require separate production validation, as do caloric and price constraints.
 */
data class RecipeCandidate(
    val id: String,
    val title: String,
    val safety: RecipeSafety,
    val compatibleDiets: Set<String>,
    val preparation: RecipePreparation,
    val tags: Set<String>,
    val evidenceStatus: RecipeEvidenceStatus = RecipeEvidenceStatus.GENERATED_TEMPLATE
)
enum class CandidateRejection { ALLERGEN, EXCLUDED_INGREDIENT, UNKNOWN_COMPOSITION, DIET, EQUIPMENT_OR_TIME_OR_EFFORT }
data class RankedCandidate(val recipe: RecipeCandidate, val score: Int)
data class RankedCandidates(
    val ranked: List<RankedCandidate>,
    val rejected: Map<String, CandidateRejection>
)

object RecipeRanker {
    private fun terms(value: String): Set<String> =
        value.split(',', ';', '\n').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun rank(
        profile: FoodProfile,
        recipes: List<RecipeCandidate>,
        votes: Map<String, TasteRating>,
        repeatCounts: Map<String, Int> = emptyMap()
    ): RankedCandidates {
        val allergies = terms(profile.allergens)
        val excluded = terms(profile.disliked)
        val weights = TasteSurvey.tasteWeights(votes)
        val accepted = mutableListOf<RankedCandidate>()
        val rejections = linkedMapOf<String, CandidateRejection>()

        recipes.forEach { recipe ->
            // Fail closed for missing ingredient/allergen completeness.
            val allergens = recipe.safety.allergenIds.map { it.trim().lowercase() }.toSet()
            val ingredients = recipe.safety.ingredientIds.map { it.trim().lowercase() }.toSet()
            val rejection = when {
                !recipe.safety.completeIngredientAndAllergenData ->
                    CandidateRejection.UNKNOWN_COMPOSITION
                // Ingredient names may appear without explicit allergen tags.
                allergies.any { it in allergens || it in ingredients } -> CandidateRejection.ALLERGEN
                excluded.any { it in ingredients } -> CandidateRejection.EXCLUDED_INGREDIENT
                profile.diet != "Всеядный" && profile.diet !in recipe.compatibleDiets ->
                    CandidateRejection.DIET
                !RecipePreparationRules.eligible(recipe.preparation, profile) ->
                    CandidateRejection.EQUIPMENT_OR_TIME_OR_EFFORT
                else -> null
            }
            if (rejection != null) {
                rejections[recipe.id] = rejection
            } else {
                val preferenceScore = recipe.tags.sumOf { weights[it] ?: 0 }
                val difficulty = RecipePreparationRules.classify(recipe.preparation)
                val effortBonus = if (difficulty == profile.cookingEffort) 3 else 0
                val penalty = (repeatCounts[recipe.id] ?: 0).coerceAtLeast(0).coerceAtMost(10) * 8
                // The authored drafts have distinct instructions; keep the generic templates
                // as fallbacks. Unverified is NOT equivalent to nutritionally safe.
                val draftBonus = if (recipe.evidenceStatus == RecipeEvidenceStatus.CURATED_DRAFT) 12 else 0
                accepted.add(RankedCandidate(recipe, preferenceScore * 4 + effortBonus
                    + draftBonus - penalty))
            }
        }
        // Stable deterministic ranking; identical scores do not cause random order shifts.
        return RankedCandidates(accepted.sortedWith(compareByDescending<RankedCandidate> { it.score }
            .thenBy { it.recipe.id }), rejections)
    }
}
