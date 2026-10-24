package com.foodplan.domain

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * M2 working queue after the 24.10.2026 correction.
 *
 * Each of 150 original authored recipes can be reviewed against a published
 * method and its approximate, source-linked nutritional calculation.
 * There is NO mandatory physical trial, weighed finished dish, dual human
 * reviewer or per-recipe Russian retailer SKU-label certification.
 *
 * A generated CSV is only a review QUEUE: blank source fields are never
 * interpreted as proof or silently promoted to a verified recipe.
 */
object M2DocumentaryReviewQueue {
    private fun csv(value: Any?): String =
        "\"" + (value?.toString() ?: "").replace("\"", "\"\"")
            .replace("\r", " ").replace("\n", " ") + "\""

    fun generateCsv(curated: List<OfflineRecipe> = OfflineRecipes.curatedDrafts): String {
        require(curated.size == 150)
        require(curated.map { it.id }.distinct().size == 150)
        require(curated.all { it.evidenceStatus == RecipeEvidenceStatus.CURATED_DRAFT })
        val header = listOf(
            "recipe_id",
            "recipe_fingerprint_sha256",
            "recipe_title",
            "raw_ingredient_weights_and_source_references",
            "preparation_steps",
            "raw_ingredient_approximate_kcal_not_finished_lab_measurement",
            "published_method_source",
            "method_source_snapshot_sha256",
            "captured_method_excerpt_utf8",
            "method_applicability_notes",
            "review_record_reference",
            "nutrition_basis",
            "nutrition_calculation_notes",
            "independent_raw_kcal",
            "independent_raw_protein_g",
            "independent_raw_fat_g",
            "independent_raw_carbs_g",
            "published_ingredient_factor_sources",
            "estimated_finished_yield_grams_optional",
            "estimated_yield_source_optional",
            "physical_cooking_trial_reference_optional",
            "documentary_review_status"
        )
        val lines = mutableListOf(header.joinToString(",") { csv(it) })
        for (recipe in curated.sortedBy { it.id }) {
            val reviewed = M2PublishedMethodReviews.reviews[recipe.id]
            val status = if (reviewed == null) "PENDING_DOCUMENTARY_REVIEW"
                else if (RecipeEvidenceAudit.inspect(recipe, reviewed).documentedReady)
                    "DOCUMENTARY_REVIEWED"
                else "INVALID_OR_STALE_REVIEW"
            val ingredientRows = recipe.ingredients.joinToString(" | ") { part ->
                val ingredient = OfflineRecipes.ingredients.getValue(part.id)
                val basis = IngredientWeightBasisRegistry.basisFor(part.id)
                    ?.let(IngredientWeightBasisRegistry::russianLabel)
                    ?: "UNKNOWN_EDIBLE_BASIS"
                "${part.id}:${part.grams}g (${ingredient.name}; ${basis}; " +
                    "${ingredient.nutrientSourceReference ?: "NO_REFERENCE"})"
            }
            val row = listOf(
                recipe.id,
                RecipeEvidenceAudit.fingerprint(recipe),
                recipe.name,
                ingredientRows,
                recipe.steps.mapIndexed { index, step -> "${index + 1}. ${step}" }.joinToString(" | "),
                String.format(Locale.ROOT, "%.3f", OfflineRecipes.nutrition(recipe).kcal),
                reviewed?.publishedMethodSource ?: "",
                reviewed?.methodSnapshotSha256 ?: "",
                reviewed?.methodSnapshotText ?: "",
                reviewed?.methodApplicabilityNotes ?: "",
                reviewed?.reviewRecordReference ?: "",
                (reviewed?.nutritionBasis ?: RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE).name,
                reviewed?.nutritionCalculationNotes ?: "",
                reviewed?.independentlyRecalculatedRawNutrition?.kcal ?: "",
                reviewed?.independentlyRecalculatedRawNutrition?.protein ?: "",
                reviewed?.independentlyRecalculatedRawNutrition?.fat ?: "",
                reviewed?.independentlyRecalculatedRawNutrition?.carbs ?: "",
                reviewed?.publishedCookingFactorSources?.entries
                    ?.joinToString(" | ") { "${it.key}:${it.value}" } ?: "",
                reviewed?.estimatedFinishedYieldGrams ?: "",
                reviewed?.estimatedYieldSourceReference ?: "",
                reviewed?.physicalTrialSourceReference ?: "",
                status
            )
            require(row.size == header.size)
            lines += row.joinToString(",") { csv(it) }
        }
        return lines.joinToString("\n", postfix = "\n")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1 && args[0].endsWith(".csv"))
        val output = Path.of(args[0])
        output.parent?.let { Files.createDirectories(it) }
        Files.writeString(output, generateCsv(), StandardCharsets.UTF_8)
        val current = RecipeEvidenceAudit.report()
        println("M2 documentary review queue: 150 authored recipes; " +
            "${current.documentedReviewed} source-reviewed, " +
            "${current.physicalTrialsRecorded} optional physical trials.")
        println("M2 documentary release gate: " +
            if (current.meetsM2Gate) "PASS" else "OPEN")
    }
}
