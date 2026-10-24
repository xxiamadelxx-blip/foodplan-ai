package com.foodplan.domain

/**
 * Human shopping names are deliberately separate from scientific nutrient labels.
 * A USDA raw weight basis belongs to calculations, never the user's shopping title.
 */
object ShoppingDisplayNames {
    private val titles: Map<String, String> = mapOf(
        "rice" to "Рис",
        "buckwheat" to "Гречка",
        "pasta" to "Макароны",
        "potato" to "Картофель",
        "oats" to "Овсяные хлопья",
        "millet" to "Пшено",
        "milk" to "Молоко",
        "apple" to "Яблоки",
        "banana" to "Бананы",
        "pear" to "Груши",
        "berries" to "Замороженная клубника",
        "chicken" to "Куриное филе",
        "turkey" to "Филе индейки",
        "beef" to "Говядина",
        "pork" to "Свинина",
        "cod" to "Треска",
        "salmon" to "Лосось",
        "tofu" to "Тофу",
        "chickpeas" to "Консервированный нут",
        "broccoli" to "Брокколи",
        "carrot" to "Морковь",
        "zucchini" to "Кабачки",
        "cabbage" to "Капуста",
        "tomato" to "Помидоры",
        "mushroom" to "Шампиньоны",
        "oil" to "Подсолнечное масло",
        "egg" to "Яйца",
        "cottage_cheese" to "Творог",
        "flour" to "Пшеничная мука",
        "onion" to "Лук",
        "garlic" to "Чеснок",
        "cucumber" to "Огурцы",
        "bell_pepper" to "Сладкий перец",
        "lemon" to "Лимоны",
        "lentils" to "Чечевица",
        "yogurt" to "Натуральный йогурт",
        "cheese" to "Сыр Гауда"
    )

    fun forIngredient(ingredientId: String): String =
        titles[ingredientId] ?: ingredientId.replace('_', ' ')

    fun supportedIngredientIds(): Set<String> = titles.keys
}
