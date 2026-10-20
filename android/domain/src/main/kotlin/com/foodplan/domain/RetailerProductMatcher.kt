package com.foodplan.domain

/** Prevents applying a price of an unrelated SKU to a grocery ingredient. */
object RetailerProductMatcher {
    private val aliases = mapOf(
        "rice" to listOf("рис"), "buckwheat" to listOf("гречка","гречневая крупа"),
        "pasta" to listOf("макароны","спагетти","паста"),
        "potato" to listOf("картофель","картошка"),
        "oats" to listOf("овсяные хлопья","геркулес"),
        "millet" to listOf("пшено"), "milk" to listOf("молоко"),
        "apple" to listOf("яблоки","яблоко"),"banana" to listOf("банан","бананы"),
        "pear" to listOf("груша","груши"),
        "berries" to listOf("клубника","земляника"),
        "chicken" to listOf("курица","куриное","куриный","куриной"),
        "turkey" to listOf("индейка","индейки","индюш"),
        "beef" to listOf("говядина","говяж"),
        "pork" to listOf("свинина","свин"),
        "cod" to listOf("треска","трески"),
        "salmon" to listOf("лосось","семга","сёмга"),
        "tofu" to listOf("тофу"),"chickpeas" to listOf("нут"),
        "broccoli" to listOf("брокколи"),"carrot" to listOf("морковь","морков"),
        "zucchini" to listOf("кабачок","кабачки"),
        "cabbage" to listOf("капуста","капуст"),
        "tomato" to listOf("помидоры","помидор","томаты","томат"),
        "mushroom" to listOf("шампиньоны","шампиньон","грибы"),
        "oil" to listOf("масло подсолнечное","подсолнечное масло")
    )

    fun matchesIngredient(ingredientId: String, productName: String): Boolean {
        val fragments=aliases[ingredientId] ?: return false
        val title=productName.lowercase().replace('ё','е')
        // The food-composition record is for a specific physical state, not for
        // a prepared meal or similarly named sauce. Ambiguous SKUs fail closed.
        val processed = Regex(
            """(?<![\p{L}])(?:отварн[а-я]*|варен[а-я]*|жарен[а-я]*|готов[а-я]*|копчен[а-я]*|маринован[а-я]*|пюре|чипсы|фри|в соусе|в сиропе|с овощами|с мясом|с курицей)(?![\p{L}])"""
        )
        val basis=IngredientWeightBasisRegistry.basisFor(ingredientId)
        if (basis in setOf(IngredientWeightBasis.RAW_EDIBLE,
                IngredientWeightBasis.DRY_UNCOOKED,
                IngredientWeightBasis.FROZEN_UNCOOKED) &&
            processed.containsMatchIn(title)) return false
        // Nutritional evidence for 'berries' is specifically frozen, unsweetened
        // strawberries. Not fresh raspberries, mixed berries or sweetened packs.
        if (ingredientId=="berries" &&
            (!title.contains("заморож") || title.contains("с сахаром") ||
                title.contains("сироп") || title.contains("смесь"))) return false
        if (ingredientId=="pasta" &&
            (title.contains("паста томат") || title.contains("паста орех") ||
                title.contains("паста шоколад"))) return false
        return fragments.any { term ->
            val escaped=Regex.escape(term.lowercase().replace('ё','е'))
            Regex("(?<![\\p{L}])$escaped(?![\\p{L}])").containsMatchIn(title)
        }
    }
}
