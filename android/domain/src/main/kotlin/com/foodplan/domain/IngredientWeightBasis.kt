package com.foodplan.domain

/**
 * Recipe grams must describe the physical state of the ingredient.
 *
 * This inventory is an authored interpretation of the current recipe catalogue,
 * NOT evidence that the referenced food-composition numbers are source-verified.
 */
enum class IngredientWeightBasis {
    RAW_EDIBLE, DRY_UNCOOKED, DRAINED_READY_TO_EAT,
    PREPACKAGED_READY_TO_EAT, FROZEN_UNCOOKED
}
object IngredientWeightBasisRegistry {
    private val bases = mapOf(
        "rice" to IngredientWeightBasis.DRY_UNCOOKED,
        "buckwheat" to IngredientWeightBasis.DRY_UNCOOKED,
        "pasta" to IngredientWeightBasis.DRY_UNCOOKED,
        "oats" to IngredientWeightBasis.DRY_UNCOOKED,
        "millet" to IngredientWeightBasis.DRY_UNCOOKED,
        "flour" to IngredientWeightBasis.DRY_UNCOOKED,
        "lentils" to IngredientWeightBasis.DRY_UNCOOKED,
        "chickpeas" to IngredientWeightBasis.DRAINED_READY_TO_EAT,
        "berries" to IngredientWeightBasis.FROZEN_UNCOOKED,
        "potato" to IngredientWeightBasis.RAW_EDIBLE,
        "apple" to IngredientWeightBasis.RAW_EDIBLE,
        "banana" to IngredientWeightBasis.RAW_EDIBLE,
        "pear" to IngredientWeightBasis.RAW_EDIBLE,
        "chicken" to IngredientWeightBasis.RAW_EDIBLE,
        "turkey" to IngredientWeightBasis.RAW_EDIBLE,
        "beef" to IngredientWeightBasis.RAW_EDIBLE,
        "pork" to IngredientWeightBasis.RAW_EDIBLE,
        "cod" to IngredientWeightBasis.RAW_EDIBLE,
        "salmon" to IngredientWeightBasis.RAW_EDIBLE,
        "broccoli" to IngredientWeightBasis.RAW_EDIBLE,
        "carrot" to IngredientWeightBasis.RAW_EDIBLE,
        "zucchini" to IngredientWeightBasis.RAW_EDIBLE,
        "cabbage" to IngredientWeightBasis.RAW_EDIBLE,
        "tomato" to IngredientWeightBasis.RAW_EDIBLE,
        "mushroom" to IngredientWeightBasis.RAW_EDIBLE,
        "onion" to IngredientWeightBasis.RAW_EDIBLE,
        "garlic" to IngredientWeightBasis.RAW_EDIBLE,
        "cucumber" to IngredientWeightBasis.RAW_EDIBLE,
        "bell_pepper" to IngredientWeightBasis.RAW_EDIBLE,
        "lemon" to IngredientWeightBasis.RAW_EDIBLE,
        "egg" to IngredientWeightBasis.RAW_EDIBLE,
        "milk" to IngredientWeightBasis.PREPACKAGED_READY_TO_EAT,
        "tofu" to IngredientWeightBasis.PREPACKAGED_READY_TO_EAT,
        "oil" to IngredientWeightBasis.PREPACKAGED_READY_TO_EAT,
        "cottage_cheese" to IngredientWeightBasis.PREPACKAGED_READY_TO_EAT,
        "yogurt" to IngredientWeightBasis.PREPACKAGED_READY_TO_EAT,
        "cheese" to IngredientWeightBasis.PREPACKAGED_READY_TO_EAT
    )

    fun basisFor(id: String): IngredientWeightBasis? = bases[id]

    fun catalogueCoverage(ids: Collection<String>): Set<String> = ids.filterNot {
        bases.containsKey(it)
    }.toSet()

    /** User-facing description; it does NOT imply weighed cooked yield. */
    fun russianLabel(basis: IngredientWeightBasis): String = when(basis) {
        IngredientWeightBasis.RAW_EDIBLE -> "сырой продукт, съедобная часть"
        IngredientWeightBasis.DRY_UNCOOKED -> "сухой продукт до приготовления"
        IngredientWeightBasis.DRAINED_READY_TO_EAT -> "готовый продукт без жидкости"
        IngredientWeightBasis.PREPACKAGED_READY_TO_EAT -> "продукт в исходном виде из упаковки"
        IngredientWeightBasis.FROZEN_UNCOOKED -> "замороженный продукт до готовки"
    }
}
