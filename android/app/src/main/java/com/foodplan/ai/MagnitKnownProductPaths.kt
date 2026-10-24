package com.foodplan.ai

import com.foodplan.domain.MagnitPackOnlySkuCatalog

/**
 * Unified on-device discovery registry. All known public product-page
 * PATHS have exactly one canonical source: MagnitKnownSkuCatalog.
 *
 * A gram quantity is only an expected raw retail pack mass. Actual name,
 * mass and live price must be read from the official page each time, and
 * price quoting can still reject gross-vs-edible mismatches.
 */
internal object MagnitKnownProductPaths {
    private val expectedMassGrams: Map<String, Int> = mapOf(
        "rice" to 800,
        "buckwheat" to 900,
        "oats" to 400,
        "millet" to 800,
        "lentils" to 450,
        "flour" to 2000,
        "pasta" to 450,
        "chicken" to 1000,
        "carrot" to 1000,
        "cottage_cheese" to 180,
        "cheese" to 180,
        "mushroom" to 400,
        "potato" to 2000,
        "cucumber" to 500,
        "tomato" to 600,
        "broccoli" to 400,
        "cod" to 600,
        "tofu" to 250,
        "yogurt" to 160,
        "salmon" to 400,
        "onion" to 700,
        "garlic" to 100,
        "cabbage" to 3000,
        "apple" to 800,
        "turkey" to 500,
        "lemon" to 400,
        "pear" to 700,
        "bell_pepper" to 300,
        "zucchini" to 700,
        "milk" to 900,
        "beef" to 260
    )

    val knownIngredientIds: Set<String> = MagnitKnownSkuCatalog.urls.keys

    fun urlFor(ingredientId: String): String? =
        MagnitKnownSkuCatalog.urlFor(ingredientId)

    /** Null when SKU uses pieces, volume, unpeeled or undrained mass. */
    fun expectedGrams(ingredientId: String): Int? =
        expectedMassGrams[ingredientId]

    init {
        val otherUnits = MagnitPackOnlySkuCatalog.entries.map { it.ingredientId }.toSet()
        require(expectedMassGrams.keys.intersect(otherUnits).isEmpty())
        require(expectedMassGrams.keys + otherUnits == knownIngredientIds)
        require(expectedMassGrams.values.all { it in 1..50_000 })
    }
}
