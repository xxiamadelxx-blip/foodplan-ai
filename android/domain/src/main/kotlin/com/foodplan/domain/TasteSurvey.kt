package com.foodplan.domain

/** The survey is taste research, NOT a recipe catalogue or nutritional information. */
data class TasteDish(val id: String, val title: String, val tags: Set<String>)
enum class TasteRating { LIKE, OK, DISLIKE, NEVER_TRIED }

object TasteSurvey {
    val dishes: List<TasteDish> = listOf(
        TasteDish("borsch", "Борщ", setOf("суп", "овощи", "свёкла", "русская")),
        TasteDish("buckwheat_chicken", "Гречка с курицей", setOf("курица", "гречка", "домашняя")),
        TasteDish("beef_stew", "Говяжий гуляш", setOf("говядина", "тушёное", "домашняя")),
        TasteDish("salmon_oven", "Запечённый лосось", setOf("рыба", "духовка", "запечённое")),
        TasteDish("fried_cod", "Жареная треска", setOf("рыба", "жареное", "хрустящее")),
        TasteDish("shrimp_pasta", "Паста с креветками", setOf("морепродукты", "паста", "итальянская")),
        TasteDish("tomato_pasta", "Паста с томатным соусом", setOf("паста", "томаты", "итальянская")),
        TasteDish("lasagna", "Лазанья", setOf("паста", "сыр", "духовка", "итальянская")),
        TasteDish("sushi", "Суши", setOf("рыба", "рис", "японская")),
        TasteDish("chicken_katsu", "Курица кацу", setOf("курица", "японская", "хрустящее", "жареное")),
        TasteDish("ramen", "Рамен", setOf("суп", "лапша", "японская")),
        TasteDish("tom_yum", "Том-ям", setOf("суп", "морепродукты", "острое", "тайская")),
        TasteDish("pad_thai", "Пад-тай", setOf("лапша", "тайская", "сладко-кислое")),
        TasteDish("curry", "Карри с овощами", setOf("овощи", "пряное", "индийская")),
        TasteDish("tacos", "Тако", setOf("мясо", "острое", "мексиканская")),
        TasteDish("burger", "Бургер", setOf("говядина", "сыр", "американская")),
        TasteDish("pizza", "Пицца", setOf("сыр", "томаты", "итальянская", "духовка")),
        TasteDish("falafel", "Фалафель", setOf("бобовые", "хрустящее", "ближневосточная")),
        TasteDish("hummus", "Хумус", setOf("бобовые", "кремовая", "ближневосточная")),
        TasteDish("caesar", "Салат Цезарь", setOf("салат", "курица", "сыр")),
        TasteDish("greek_salad", "Греческий салат", setOf("салат", "овощи", "сыр")),
        TasteDish("oatmeal", "Овсяная каша", setOf("каша", "сладкое", "завтрак")),
        TasteDish("omelette", "Омлет", setOf("яйца", "завтрак", "домашняя")),
        TasteDish("cottage_cheese", "Сырники", setOf("творог", "сладкое", "завтрак")),
        TasteDish("mushroom_risotto", "Ризотто с грибами", setOf("грибы", "рис", "итальянская", "кремовая"))
    )
    init {
        require(dishes.size == 25)
        require(dishes.map { it.id }.distinct().size == 25)
    }

    fun tasteWeights(votes: Map<String, TasteRating>): Map<String, Int> {
        val weights = mutableMapOf<String, Int>()
        dishes.forEach { dish ->
            val change = when (votes[dish.id]) {
                TasteRating.LIKE -> 3
                TasteRating.OK -> 1
                TasteRating.DISLIKE -> -3
                TasteRating.NEVER_TRIED, null -> 0
            }
            dish.tags.forEach { tag -> weights[tag] = (weights[tag] ?: 0) + change }
        }
        return weights
    }
}
