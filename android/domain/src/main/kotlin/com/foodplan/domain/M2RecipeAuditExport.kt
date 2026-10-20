package com.foodplan.domain

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Human-review export. All nutrients are provisional unless their source has
 * been checked independently. Producing this CSV NEVER changes recipe status.
 */
object M2RecipeAuditExport {
    private fun csvField(value: Any?): String =
        "\"" + (value?.toString() ?: "").replace("\"", "\"\"")
            .replace("\r", " ").replace("\n", " ") + "\""

    fun generateCsv(): String {
        val header = listOf(
            "recipe_id", "title", "meal_type", "evidence_status",
            "raw_ingredient_grams_and_basis", "declared_allergens",
            "required_equipment", "active_minutes", "total_minutes",
            "estimated_kcal_per_recipe", "nutrition_verified", "source_reference",
            "audit_issues"
        )
        val lines = mutableListOf(header.joinToString(",") { csvField(it) })
        for (recipe in OfflineRecipes.recipes.sortedBy { it.id }) {
            val parts = recipe.ingredients.joinToString(" | ") { p ->
                val label = IngredientWeightBasisRegistry.basisFor(p.id)
                    ?.let(IngredientWeightBasisRegistry::russianLabel) ?: "UNKNOWN_BASIS"
                "${p.id}:${p.grams}g ($label)"
            }
            val row = listOf(
                recipe.id, recipe.name, recipe.type.name, recipe.evidenceStatus.name,
                parts, OfflineRecipes.allergens(recipe).sorted().joinToString(" | "),
                recipe.preparation.requiredEquipment.sorted().joinToString(" | "),
                recipe.preparation.activeMinutes, recipe.preparation.totalMinutes,
                "%.1f".format(java.util.Locale.ROOT, OfflineRecipes.nutrition(recipe).kcal),
                recipe.nutritionVerified, recipe.sourceReference ?: "",
                RecipeEvidenceAudit.inspect(recipe).issues.map { it.name }.sorted()
                    .joinToString(" | ")
            )
            lines += row.joinToString(",") { csvField(it) }
        }
        return lines.joinToString("\n",postfix="\n")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) { "Specify a destination CSV path" }
        val target=Path.of(args[0])
        require(target.toString().endsWith(".csv")) { "Output must have a .csv extension" }
        target.parent?.let(Files::createDirectories)
        Files.writeString(target, generateCsv(), StandardCharsets.UTF_8)
        val report=RecipeEvidenceAudit.report()
        println("M2 REVIEW EXPORT: ${report.total} rows; generated ${report.generatedTemplates}; authored ${report.distinctDrafts}; independently verified ${report.independentlyVerified}.")
        println("M2 RELEASE GATE: ${if (report.meetsM2Gate) "PASS" else "FAIL"}")
    }
}
