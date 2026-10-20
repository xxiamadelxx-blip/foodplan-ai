package com.foodplan.domain

/**
 * M2 release gate: authored recipes are not automatically scientifically verified.
 *
 * A reference to an ingredient database is not the same as verifying the recipe
 * yield, preparation weights or whether a packaged ingredient carries allergens.
 */
enum class RecipeAuditIssue {
    UNKNOWN_INGREDIENT, INVALID_WEIGHT, INCOMPLETE_STEPS, UNKNOWN_EQUIPMENT,
    INVALID_PREPARATION, DUPLICATE_INGREDIENT,
    NUTRIENTS_UNSOURCED, ALLERGENS_UNREVIEWED, RECIPE_SOURCE_MISSING,
    UNVERIFIED_RECIPE
}
data class AuditedRecipe(val id:String, val issues:Set<RecipeAuditIssue>) {
    val verified get() = issues.isEmpty()
}
data class CatalogAudit(
    val total:Int, val generatedTemplates:Int, val distinctDrafts:Int,
    val independentlyVerified:Int, val issues:Map<String,Set<RecipeAuditIssue>>
) {
    val meetsM2Gate: Boolean get() = independentlyVerified>=150 &&
        issues.values.count { it.isEmpty() } >= 150
}
object RecipeEvidenceAudit {
    fun inspect(recipe:OfflineRecipe):AuditedRecipe {
        val issues=mutableSetOf<RecipeAuditIssue>()
        if(recipe.ingredients.isEmpty() || recipe.ingredients.any {
                it.id !in OfflineRecipes.ingredients }) issues+=RecipeAuditIssue.UNKNOWN_INGREDIENT
        if(recipe.ingredients.any { it.grams !in 1..2000 })
            issues+=RecipeAuditIssue.INVALID_WEIGHT
        if(recipe.ingredients.map { it.id }.distinct().size!=recipe.ingredients.size)
            issues+=RecipeAuditIssue.DUPLICATE_INGREDIENT
        if(recipe.steps.size<3 || recipe.steps.any { it.length<20 })
            issues+=RecipeAuditIssue.INCOMPLETE_STEPS
        if(recipe.preparation.requiredEquipment.any { it !in KitchenEquipment.choices })
            issues+=RecipeAuditIssue.UNKNOWN_EQUIPMENT
        if(recipe.preparation.totalMinutes<=0 || recipe.preparation.activeMinutes<0)
            issues+=RecipeAuditIssue.INVALID_PREPARATION
        for (part in recipe.ingredients) {
            val ingredient=OfflineRecipes.ingredients[part.id] ?: continue
            if(ingredient.nutrientSourceReference.isNullOrBlank())
                issues+=RecipeAuditIssue.NUTRIENTS_UNSOURCED
            if(!ingredient.allergenCompositionReviewed)
                issues+=RecipeAuditIssue.ALLERGENS_UNREVIEWED
        }
        if(recipe.sourceReference.isNullOrBlank())
            issues+=RecipeAuditIssue.RECIPE_SOURCE_MISSING
        if(!recipe.nutritionVerified ||
            recipe.evidenceStatus!=RecipeEvidenceStatus.INDEPENDENTLY_VERIFIED)
            issues+=RecipeAuditIssue.UNVERIFIED_RECIPE
        return AuditedRecipe(recipe.id,issues)
    }

    fun report(recipes:List<OfflineRecipe> = OfflineRecipes.recipes):CatalogAudit {
        require(recipes.map { it.id }.distinct().size==recipes.size)
        val inspected=recipes.map(::inspect)
        return CatalogAudit(
            recipes.size,
            recipes.count { it.evidenceStatus==RecipeEvidenceStatus.GENERATED_TEMPLATE },
            recipes.count { it.evidenceStatus==RecipeEvidenceStatus.CURATED_DRAFT },
            inspected.count { it.verified },
            inspected.associate { it.id to it.issues }
        )
    }
}
