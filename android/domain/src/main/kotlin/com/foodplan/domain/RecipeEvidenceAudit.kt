package com.foodplan.domain

import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * M2 evidence is documentary, not a demand to cook/weigh every recipe.
 * Source candidates are NOT accepted until a review record matches this
 * exact recipe fingerprint and provides auditable publication/calculation data.
 *
 * A recipe review can never certify the allergens/traces of an actual retail
 * SKU: that evidence belongs to the separate retailer/label safety gate.
 */
enum class RecipeNutritionBasis {
    /** Approximate input-edible-ingredient totals; NOT laboratory cooked values. */
    RAW_EDIBLE_INPUT_ESTIMATE,
    /** Ingredient-specific published cooking/retention factors are documented. */
    INGREDIENT_SPECIFIC_PUBLISHED_FACTORS
}

data class RecipeDocumentaryReview(
    val recipeId: String,
    val recipeFingerprintSha256: String,
    val publishedMethodSource: String,
    val methodSnapshotSha256: String,
    val methodApplicabilityNotes: String,
    val reviewRecordReference: String,
    val nutritionBasis: RecipeNutritionBasis,
    val nutritionCalculationNotes: String,
    /** Keys are ingredient IDs, never generic coefficients for a whole mixed dish. */
    val publishedCookingFactorSources: Map<String, String> = emptyMap(),
    /** OPTIONAL documented estimate, never a measured physical cooked yield. */
    val estimatedFinishedYieldGrams: Int? = null,
    val estimatedYieldSourceReference: String? = null,
    /** OPTIONAL genuine cooking-trial report; unrelated to release eligibility. */
    val physicalTrialSourceReference: String? = null
)

enum class RecipeAuditIssue {
    UNKNOWN_INGREDIENT, INVALID_WEIGHT, INCOMPLETE_STEPS, UNKNOWN_EQUIPMENT,
    INVALID_PREPARATION, DUPLICATE_INGREDIENT,
    NUTRIENTS_UNSOURCED, RECIPE_SOURCE_MISSING,
    UNKNOWN_WEIGHT_BASIS, UNVERIFIED_RECIPE, INVALID_EVIDENCE_REFERENCE,
    DOCUMENTARY_REVIEW_MISSING, DOCUMENTARY_REVIEW_STALE,
    METHOD_SNAPSHOT_MISSING, METHOD_COMPARISON_MISSING,
    NUTRITION_CALCULATION_UNDOCUMENTED, COOKING_FACTORS_UNSOURCED,
    INVALID_ESTIMATED_YIELD, INVALID_PHYSICAL_TRIAL,
    UNVERIFIED_COOKED_YIELD
}

/** Informational flags only. Neither requires 150 cooking trials nor clears SKU safety. */
enum class RecipeAuditNotice {
    COOKED_YIELD_NOT_MEASURED, FINISHED_YIELD_UNKNOWN,
    RAW_INPUT_NUTRITION_APPROXIMATE, SKU_LABEL_TRACES_UNKNOWN
}

data class AuditedRecipe(
    val id: String,
    val issues: Set<RecipeAuditIssue>,
    val notices: Set<RecipeAuditNotice> = emptySet(),
    val methodDocumented: Boolean = false,
    val nutritionCalculationDocumented: Boolean = false,
    val physicalTrialDocumented: Boolean = false,
    val skuLabelCleared: Boolean = false
) {
    /** A cookbook/documentary review, NOT a lab certificate or SKU allergen sign-off. */
    val documentedReady: Boolean get() =
        issues.isEmpty() && methodDocumented && nutritionCalculationDocumented

    /** Backward compatibility for existing audit clients; documentary only. */
    val verified: Boolean get() = documentedReady
}

data class CatalogAudit(
    val total: Int,
    val generatedTemplates: Int,
    val distinctDrafts: Int,
    /** Historical physical/independently attested field; NOT the M2 gate. */
    val independentlyVerified: Int,
    val issues: Map<String, Set<RecipeAuditIssue>>,
    /** Current M2 gate: documented and reviewed source/calculation for each dish. */
    val documentedReviewed: Int = 0,
    val physicalTrialsRecorded: Int = 0,
    val skuLabelsClearedByRecipes: Int = 0
) {
    val meetsM2Gate: Boolean get() = documentedReviewed >= 150
}

object RecipeEvidenceAudit {
    fun fingerprint(recipe: OfflineRecipe): String {
        // A stale cookbook review must never be reused after edits to amounts,
        // cooking steps, kitchen equipment, active/total time or dish identity.
        val stable = buildString {
            append(recipe.id).append('\n')
            append(recipe.name).append('\n')
            append(recipe.type.name).append('\n')
            recipe.ingredients.forEach { append(it.id).append(':').append(it.grams).append('\n') }
            recipe.steps.forEach { append(it).append('\n') }
            append(recipe.preparation.requiredEquipment.sorted().joinToString("|")).append('\n')
            append(recipe.preparation.activeMinutes).append(':')
            append(recipe.preparation.totalMinutes).append('\n')
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(stable.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun validSha(value: String?): Boolean =
        value != null && Regex("[0-9a-fA-F]{64}").matches(value)

    /** Syntax only: a URL is NOT proof that the external document matches. */
    private fun validEvidenceLink(value: String?): Boolean {
        if (value.isNullOrBlank() || value != value.trim()) return false
        return try {
            val uri = URI(value)
            uri.scheme == "https" && !uri.host.isNullOrBlank() &&
                uri.userInfo == null && uri.fragment == null &&
                uri.host!!.contains('.') && !uri.rawPath.isNullOrBlank()
        } catch (_: Exception) { false }
    }

    fun inspect(
        recipe: OfflineRecipe,
        review: RecipeDocumentaryReview? = null
    ): AuditedRecipe {
        val issues = mutableSetOf<RecipeAuditIssue>()
        val notices = mutableSetOf<RecipeAuditNotice>()
        if (recipe.ingredients.isEmpty() || recipe.ingredients.any {
                it.id !in OfflineRecipes.ingredients
            }) issues += RecipeAuditIssue.UNKNOWN_INGREDIENT
        if (recipe.ingredients.any { it.grams !in 1..2000 })
            issues += RecipeAuditIssue.INVALID_WEIGHT
        if (recipe.ingredients.map { it.id }.distinct().size != recipe.ingredients.size)
            issues += RecipeAuditIssue.DUPLICATE_INGREDIENT
        if (recipe.steps.size < 3 || recipe.steps.any { it.length < 20 })
            issues += RecipeAuditIssue.INCOMPLETE_STEPS
        if (recipe.preparation.requiredEquipment.any { it !in KitchenEquipment.choices })
            issues += RecipeAuditIssue.UNKNOWN_EQUIPMENT
        if (recipe.preparation.totalMinutes <= 0 || recipe.preparation.activeMinutes < 0)
            issues += RecipeAuditIssue.INVALID_PREPARATION
        if (recipe.ingredients.any { IngredientWeightBasisRegistry.basisFor(it.id) == null })
            issues += RecipeAuditIssue.UNKNOWN_WEIGHT_BASIS
        if (recipe.ingredients.any {
                OfflineRecipes.ingredients[it.id]?.nutrientSourceReference.isNullOrBlank()
            }) issues += RecipeAuditIssue.NUTRIENTS_UNSOURCED

        // Existing exact measured data remains meaningful when it is supplied,
        // but its ABSENCE never blocks ordinary documentary M2 acceptance.
        if (recipe.cookedYieldGrams == null && recipe.cookedYieldSourceReference == null) {
            notices += RecipeAuditNotice.COOKED_YIELD_NOT_MEASURED
        } else if (recipe.cookedYieldGrams == null ||
            recipe.cookedYieldGrams !in 1..15000 ||
            !validEvidenceLink(recipe.cookedYieldSourceReference)) {
            issues += RecipeAuditIssue.UNVERIFIED_COOKED_YIELD
        }
        if (recipe.sourceReference != null && !validEvidenceLink(recipe.sourceReference))
            issues += RecipeAuditIssue.INVALID_EVIDENCE_REFERENCE

        // A generated template is never part of the 150 reviewed authored dishes.
        if (recipe.evidenceStatus == RecipeEvidenceStatus.GENERATED_TEMPLATE)
            issues += RecipeAuditIssue.UNVERIFIED_RECIPE

        var methodDocumented = false
        var calculationDocumented = false
        var physicalTrial = false

        if (review == null) {
            issues += RecipeAuditIssue.DOCUMENTARY_REVIEW_MISSING
            issues += RecipeAuditIssue.RECIPE_SOURCE_MISSING
            issues += RecipeAuditIssue.NUTRITION_CALCULATION_UNDOCUMENTED
        } else {
            if (review.recipeId != recipe.id ||
                review.recipeFingerprintSha256 != fingerprint(recipe))
                issues += RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE

            if (!validEvidenceLink(review.publishedMethodSource) ||
                !validEvidenceLink(review.reviewRecordReference))
                issues += RecipeAuditIssue.INVALID_EVIDENCE_REFERENCE
            if (!validSha(review.methodSnapshotSha256))
                issues += RecipeAuditIssue.METHOD_SNAPSHOT_MISSING
            if (review.methodApplicabilityNotes.trim().length < 20)
                issues += RecipeAuditIssue.METHOD_COMPARISON_MISSING
            methodDocumented =
                RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE !in issues &&
                RecipeAuditIssue.INVALID_EVIDENCE_REFERENCE !in issues &&
                RecipeAuditIssue.METHOD_SNAPSHOT_MISSING !in issues &&
                RecipeAuditIssue.METHOD_COMPARISON_MISSING !in issues

            if (review.nutritionCalculationNotes.trim().length < 20)
                issues += RecipeAuditIssue.NUTRITION_CALCULATION_UNDOCUMENTED
            when (review.nutritionBasis) {
                RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE ->
                    notices += RecipeAuditNotice.RAW_INPUT_NUTRITION_APPROXIMATE
                RecipeNutritionBasis.INGREDIENT_SPECIFIC_PUBLISHED_FACTORS -> {
                    val ingredientIds = recipe.ingredients.map { it.id }.toSet()
                    if (review.publishedCookingFactorSources.isEmpty() ||
                        review.publishedCookingFactorSources.keys.any { it !in ingredientIds } ||
                        review.publishedCookingFactorSources.values.any { !validEvidenceLink(it) }) {
                        issues += RecipeAuditIssue.COOKING_FACTORS_UNSOURCED
                    }
                }
            }
            calculationDocumented =
                RecipeAuditIssue.NUTRITION_CALCULATION_UNDOCUMENTED !in issues &&
                RecipeAuditIssue.COOKING_FACTORS_UNSOURCED !in issues &&
                RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE !in issues

            // An estimated result must NEVER be placed into the physical
            // recipe.cookedYieldGrams field. Unknown estimated yield is fine.
            if (review.estimatedFinishedYieldGrams == null &&
                review.estimatedYieldSourceReference == null) {
                notices += RecipeAuditNotice.FINISHED_YIELD_UNKNOWN
            } else if (review.estimatedFinishedYieldGrams == null ||
                review.estimatedFinishedYieldGrams !in 1..15000 ||
                !validEvidenceLink(review.estimatedYieldSourceReference)) {
                issues += RecipeAuditIssue.INVALID_ESTIMATED_YIELD
            }
            if (review.physicalTrialSourceReference != null) {
                if (validEvidenceLink(review.physicalTrialSourceReference))
                    physicalTrial = true
                else issues += RecipeAuditIssue.INVALID_PHYSICAL_TRIAL
            }
        }

        // "Known ingredient allergens" != supplier label clearance.
        // A recipe audit must never promote a real physical SKU to safe.
        if (recipe.ingredients.any {
                OfflineRecipes.ingredients[it.id]?.allergenCompositionReviewed != true
            }) notices += RecipeAuditNotice.SKU_LABEL_TRACES_UNKNOWN

        return AuditedRecipe(
            recipe.id, issues, notices, methodDocumented,
            calculationDocumented, physicalTrial, skuLabelCleared = false
        )
    }

    fun report(
        recipes: List<OfflineRecipe> = OfflineRecipes.recipes,
        documentaryReviews: Map<String, RecipeDocumentaryReview> = emptyMap()
    ): CatalogAudit {
        require(recipes.map { it.id }.distinct().size == recipes.size)
        val inspected = recipes.map { recipe ->
            recipe to inspect(recipe, documentaryReviews[recipe.id])
        }
        return CatalogAudit(
            total = recipes.size,
            generatedTemplates = recipes.count {
                it.evidenceStatus == RecipeEvidenceStatus.GENERATED_TEMPLATE
            },
            distinctDrafts = recipes.count {
                it.evidenceStatus == RecipeEvidenceStatus.CURATED_DRAFT
            },
            // Old independently verified status does not substitute for source
            // review. Physical testing is OPTIONAL for new M2 acceptance.
            independentlyVerified = inspected.count { (recipe, audit) ->
                recipe.evidenceStatus == RecipeEvidenceStatus.INDEPENDENTLY_VERIFIED &&
                    audit.documentedReady && audit.physicalTrialDocumented
            },
            issues = inspected.associate { (recipe, audit) -> recipe.id to audit.issues },
            documentedReviewed = inspected.count { (recipe, audit) ->
                recipe.evidenceStatus != RecipeEvidenceStatus.GENERATED_TEMPLATE &&
                    audit.documentedReady
            },
            physicalTrialsRecorded = inspected.count { it.second.physicalTrialDocumented },
            skuLabelsClearedByRecipes = 0
        )
    }
}
