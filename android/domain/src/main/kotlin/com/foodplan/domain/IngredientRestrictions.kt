package com.foodplan.domain

/** Local ingredient synonym dictionary for explicit disliked foods, independent of taste votes. */
object IngredientRestrictions {
    private val aliases = mapOf(
        "курица" to setOf("chicken"), "куриное мясо" to setOf("chicken"),
        "индейка" to setOf("turkey"), "говядина" to setOf("beef"),
        "свинина" to setOf("pork"), "мясо" to setOf("chicken","turkey","beef","pork"),
        "рыба" to setOf("cod","salmon"), "треска" to setOf("cod"),
        "лосось" to setOf("salmon"),
        "тофу" to setOf("tofu"), "соя" to setOf("tofu"),
        "молоко" to setOf("milk"), "лактоза" to setOf("milk"),
        "рис" to setOf("rice"), "гречка" to setOf("buckwheat"),
        "овсянка" to setOf("oats"), "овсяные хлопья" to setOf("oats"),
        "пшено" to setOf("millet"),
        "макароны" to setOf("pasta"), "паста" to setOf("pasta"),
        "картофель" to setOf("potato"), "картошка" to setOf("potato"),
        "брокколи" to setOf("broccoli"), "морковь" to setOf("carrot"),
        "кабачки" to setOf("zucchini"), "кабачок" to setOf("zucchini"),
        "капуста" to setOf("cabbage"), "помидоры" to setOf("tomato"),
        "томат" to setOf("tomato"), "грибы" to setOf("mushroom"),
        "шампиньоны" to setOf("mushroom"),
        "яблоко" to setOf("apple"), "яблоки" to setOf("apple"),
        "банан" to setOf("banana"), "бананы" to setOf("banana"),
        "груша" to setOf("pear"), "груши" to setOf("pear"),
        "ягоды" to setOf("berries"),
        "нут" to setOf("chickpeas"),
        "масло" to setOf("oil"), "растительное масло" to setOf("oil")
    )
    data class Restriction(val ingredientIds: Set<String>, val unknown: List<String>)
    fun parse(input: String): Restriction {
        val values = input.split(',', ';', '\n').map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        val unknown = values.filterNot { it in aliases || it in OfflineRecipes.ingredients }
        val ids = values.flatMap { aliases[it] ?: setOf(it).takeIf { it.first() in OfflineRecipes.ingredients }
            ?: emptySet() }.toSet()
        return Restriction(ids, unknown)
    }
}
