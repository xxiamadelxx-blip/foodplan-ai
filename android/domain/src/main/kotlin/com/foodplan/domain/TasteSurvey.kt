package com.foodplan.domain

/** The survey is taste research, NOT a recipe catalogue or nutritional information. */
data class TasteDish(val id: String, val title: String, val tags: Set<String>)
enum class TasteRating { LIKE, OK, DISLIKE, NEVER_TRIED }

object TasteSurvey {
    // v2: familiar everyday meals dominate; unchanged dish IDs retain votes.
    // Never reuse an old ID for a different dish.
    const val CATALOG_VERSION = 2
    val dishes: List<TasteDish> = listOf(
        TasteDish("oatmeal", "Овсяная каша", setOf("каша", "мягкое", "завтрак")),
        TasteDish("omelette", "Омлет", setOf("яйца", "жареное", "завтрак")),
        TasteDish("cottage_cheese", "Сырники", setOf("творог", "сладкое", "завтрак")),
        TasteDish("cheese_sandwich_v2", "Бутерброд с сыром", setOf("хлеб", "сыр", "быстрое")),
        TasteDish("borsch", "Борщ", setOf("суп", "овощи", "свёкла", "русская")),
        TasteDish("chicken_soup_v2", "Куриный суп", setOf("суп", "курица", "домашняя")),
        TasteDish("cutlets_mash_v2", "Котлеты с пюре", setOf("мясо", "картофель", "домашняя")),
        TasteDish("buckwheat_chicken", "Гречка с курицей", setOf("курица", "гречка", "домашняя")),
        TasteDish("rice_vegetables_v2", "Рис с овощами", setOf("рис", "овощи", "домашняя")),
        TasteDish("tomato_pasta", "Паста с томатным соусом", setOf("паста", "томаты", "итальянская")),
        TasteDish("chicken_pilaf_v2", "Плов с курицей", setOf("рис", "курица", "пряное")),
        TasteDish("beef_stew", "Говяжий гуляш", setOf("говядина", "тушёное", "домашняя")),
        TasteDish("oven_fish_v2", "Рыба, запечённая в духовке", setOf("рыба", "духовка", "запечённое")),
        TasteDish("tomato_cucumber_salad_v2", "Салат из огурцов и помидоров", setOf("салат", "овощи", "свежее")),
        TasteDish("potato_mushrooms_v2", "Картофель с грибами", setOf("картофель", "грибы", "жареное")),
        TasteDish("spicy_chicken_v2", "Острая курица", setOf("курица", "острое")),
        TasteDish("sweet_sour_chicken_v2", "Курица в кисло-сладком соусе", setOf("курица", "сладко-кислое")),
        TasteDish("crispy_chicken_v2", "Курица с хрустящей корочкой", setOf("курица", "хрустящее")),
        TasteDish("creamy_mushrooms_v2", "Грибы в сливочном соусе", setOf("грибы", "кремовая")),
        TasteDish("smoky_potato_v2", "Картофель с копчёной паприкой", setOf("картофель", "копчёное")),
        TasteDish("pizza", "Пицца", setOf("сыр", "томаты", "итальянская", "духовка")),
        TasteDish("burger", "Бургер", setOf("говядина", "сыр", "американская")),
        TasteDish("fish_rolls_v2", "Роллы с рыбой", setOf("рыба", "рис", "японская")),
        TasteDish("chicken_curry_v2", "Курица карри", setOf("курица", "пряное")),
        TasteDish("vegetable_noodles_v2", "Лапша с овощами на сковороде", setOf("лапша", "овощи", "жареное"))
    )
    /** Old ratings for replaced IDs are ignored, never transferred to another dish. */
    fun migrateVotes(saved: Map<String, TasteRating>): Map<String, TasteRating> =
        saved.filterKeys { id -> dishes.any { it.id == id } }

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
