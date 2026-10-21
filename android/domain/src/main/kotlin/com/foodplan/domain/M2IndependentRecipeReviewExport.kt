package com.foodplan.domain

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * Editable evidence queue for INDEPENDENT M2 reviewers, not a self-certification.
 *
 * Only individually authored drafts are exported. All external proof columns
 * stay blank until actual source snapshots, independent method review, cooked
 * yield measurement and real supplier-specific allergen records exist.
 *
 * Reference raw ingredient calories do not equal independently verified
 * nutritional values of a cooked dish or of a Russian retail SKU.
 */
object M2IndependentRecipeReviewExport {
    private fun csv(value: Any?): String =
        "\"" + (value?.toString() ?: "").replace("\"", "\"\"")
            .replace("\r", " ").replace("\n", " ") + "\""

    data class QueueRecord(
        val recipeId: String,
        val title: String,
        val meal: MealType,
        val rawInputGrams: Int,
        val ingredientRows: String,
        val fullMethod: String,
        val preliminaryInputKcal: Double,
        val sourceLinkedIngredientCount: Int,
        val requiredIngredientCount: Int,
        val requiredEquipment: String,
        val activeMinutes: Int,
        val totalMinutes: Int,
        val possibleAllergensFromReferenceIngredients: String,
        val cookingYieldMissing: Boolean,
        val recipeTechnologySourceMissing: Boolean,
        val packageTraceEvidenceMissing: Boolean,
        val independentVerdict: String = "PENDING_EXTERNAL_REVIEW"
    )

    fun records(
        curated: List<OfflineRecipe> = OfflineRecipes.curatedDrafts
    ): List<QueueRecord> {
        require(curated.size == curated.map { it.id }.distinct().size)
        require(curated.all { it.evidenceStatus == RecipeEvidenceStatus.CURATED_DRAFT })
        return curated.sortedBy { it.id }.map { recipe ->
            QueueRecord(
                recipeId = recipe.id,
                title = recipe.name,
                meal = recipe.type,
                rawInputGrams = recipe.ingredients.sumOf { it.grams },
                ingredientRows = recipe.ingredients.joinToString(" | ") { part ->
                    val ingredient = OfflineRecipes.ingredients.getValue(part.id)
                    val basis = IngredientWeightBasisRegistry.basisFor(part.id)
                        ?.let(IngredientWeightBasisRegistry::russianLabel)
                        ?: "UNKNOWN_INPUT_BASIS"
                    "${part.id}:${part.grams} г (${ingredient.name}; $basis; " +
                        "${ingredient.nutrientSourceReference ?: "NO_REFERENCE_NUTRIENTS"})"
                },
                fullMethod = recipe.steps.mapIndexed { index, text ->
                    "${index + 1}. $text"
                }.joinToString(" | "),
                preliminaryInputKcal = OfflineRecipes.nutrition(recipe).kcal,
                sourceLinkedIngredientCount = recipe.ingredients.count {
                    OfflineRecipes.ingredients[it.id]?.nutrientSourceReference != null
                },
                requiredIngredientCount = recipe.ingredients.size,
                requiredEquipment = recipe.preparation.requiredEquipment.sorted().joinToString(" | "),
                activeMinutes = recipe.preparation.activeMinutes,
                totalMinutes = recipe.preparation.totalMinutes,
                possibleAllergensFromReferenceIngredients =
                    OfflineRecipes.allergens(recipe).sorted().joinToString(" | "),
                cookingYieldMissing =
                    recipe.cookedYieldGrams == null || recipe.cookedYieldSourceReference == null,
                recipeTechnologySourceMissing = recipe.sourceReference == null,
                packageTraceEvidenceMissing = recipe.ingredients.any {
                    !OfflineRecipes.ingredients.getValue(it.id).allergenCompositionReviewed
                }
            )
        }
    }

    fun generateCsv(
        curated: List<OfflineRecipe> = OfflineRecipes.curatedDrafts
    ): String {
        val header = listOf(
            "recipe_id", "recipe_title", "meal", "evidence_status",
            "raw_input_weight_grams_not_cooked_yield",
            "ingredient_quantities_raw_basis_and_reference_sources",
            "full_preparation_steps", "equipment",
            "active_minutes", "total_minutes",
            "preliminary_raw_input_energy_kcal_not_certified_cooked",
            "sourced_reference_ingredient_count", "ingredient_count",
            "reference_ingredient_allergens_not_sku_trace_clearance",
            "missing_measured_cooked_yield",
            "missing_independent_cooking_method_source",
            "missing_exact_product_allergen_trace_review",
            "external_method_source_and_snapshot_sha256",
            "independent_reviewer_identifier",
            "second_review_identifier",
            "cooked_dish_measured_weight_and_trial_source",
            "cooking_yield_or_retention_calculation_and_source",
            "exact_sku_manufacturer_gtin_trace_label_evidence",
            "independent_verdict"
        )
        val lines = mutableListOf(header.joinToString(",") { csv(it) })
        for (row in records(curated)) {
            val fields = listOf(
                row.recipeId, row.title, row.meal.name, "CURATED_DRAFT",
                row.rawInputGrams, row.ingredientRows, row.fullMethod,
                row.requiredEquipment, row.activeMinutes, row.totalMinutes,
                String.format(Locale.ROOT, "%.3f", row.preliminaryInputKcal),
                row.sourceLinkedIngredientCount, row.requiredIngredientCount,
                row.possibleAllergensFromReferenceIngredients,
                row.cookingYieldMissing, row.recipeTechnologySourceMissing,
                row.packageTraceEvidenceMissing,
                "", "", "", "", "", "",
                row.independentVerdict
            )
            require(fields.size == header.size)
            lines += fields.joinToString(",") { csv(it) }
        }
        return lines.joinToString("\n", postfix = "\n")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) { "Specify CSV output path" }
        val path = Path.of(args[0])
        require(path.toString().endsWith(".csv"))
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, generateCsv(), StandardCharsets.UTF_8)
        val rows = records()
        println("M2 independent review queue: ${rows.size} authored drafts; " +
            "${rows.count { it.cookingYieldMissing }} missing cooked-yield evidence; " +
            "${rows.count { it.recipeTechnologySourceMissing }} missing external method review; " +
            "${rows.count { it.packageTraceEvidenceMissing }} missing SKU-specific allergen proofs.")
        println("M2 RELEASE GATE: FAIL until >=150 independently reviewed recipes, never self-certify CSV.")
    }
}
