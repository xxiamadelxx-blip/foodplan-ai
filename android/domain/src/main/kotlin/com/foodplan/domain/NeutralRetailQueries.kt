package com.foodplan.domain

/** Generic food search: no brand, seller SKU or user-private data. */
object NeutralRetailQueries {
    private val definitions: Map<String, Pair<String, String>> = mapOf(
        "rice" to ("крупы" to "рис"),
        "buckwheat" to ("крупы" to "гречка"),
        "pasta" to ("макароны" to "макароны"),
        "potato" to ("овощи" to "картофель"),
        "oats" to ("крупы" to "овсяные хлопья"),
        "millet" to ("крупы" to "пшено"),
        "milk" to ("молочные продукты" to "молоко"),
        "apple" to ("фрукты" to "яблоки"),
        "banana" to ("фрукты" to "бананы"),
        "pear" to ("фрукты" to "груши"),
        "berries" to ("ягоды" to "клубника замороженная"),
        "chicken" to ("мясо и птица" to "куриное филе"),
        "turkey" to ("мясо и птица" to "филе индейки"),
        "beef" to ("мясо" to "говядина"),
        "pork" to ("мясо" to "свинина"),
        "cod" to ("рыба" to "треска"),
        "salmon" to ("рыба" to "лосось"),
        "tofu" to ("растительные белки" to "тофу"),
        "chickpeas" to ("консервы" to "нут консервированный"),
        "broccoli" to ("овощи" to "брокколи"),
        "carrot" to ("овощи" to "морковь"),
        "zucchini" to ("овощи" to "кабачок"),
        "cabbage" to ("овощи" to "капуста белокочанная"),
        "tomato" to ("овощи" to "помидоры"),
        "mushroom" to ("грибы" to "шампиньоны"),
        "oil" to ("масла" to "масло подсолнечное"),
        "egg" to ("яйца" to "яйца куриные"),
        "cottage_cheese" to ("молочные продукты" to "творог 5%"),
        "flour" to ("мука" to "мука пшеничная"),
        "onion" to ("овощи" to "лук репчатый"),
        "garlic" to ("овощи" to "чеснок"),
        "cucumber" to ("овощи" to "огурцы"),
        "bell_pepper" to ("овощи" to "перец сладкий"),
        "lemon" to ("фрукты" to "лимон"),
        "lentils" to ("крупы и бобовые" to "чечевица"),
        "yogurt" to ("молочные продукты" to "йогурт натуральный"),
        "cheese" to ("сыр" to "сыр гауда")
    )

    fun supportedIngredientIds(): Set<String> = definitions.keys

    fun fromShopping(needs: List<ShoppingNeed>): List<NeutralGroceryNeed> = needs
        .filter { it.grams > 0 }
        .map { need ->
            val (category, term) = definitions[need.ingredient.id]
                ?: throw IllegalArgumentException("No anonymous retail search policy for ${need.ingredient.id}")
            NeutralGroceryNeed(need.ingredient.id, category, listOf(term), need.grams)
        }

    fun scanPlan(needs: List<ShoppingNeed>): List<RetailScanStep> =
        RetailScanPlanner.plan(fromShopping(needs))
}
