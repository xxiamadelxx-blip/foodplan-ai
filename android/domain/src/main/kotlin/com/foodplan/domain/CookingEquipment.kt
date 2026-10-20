package com.foodplan.domain

/** Stable Russian equipment names. No manufacturer or retailer integration required. */
object KitchenEquipment {
    val choices = listOf(
        "Плита", "Духовка", "Блендер", "Микроволновка", "Аэрогриль",
        "Мультиварка", "Электрогриль", "Пароварка", "Скороварка", "Рисоварка",
        "Кухонный комбайн", "Мясорубка", "Миксер", "Погружной блендер",
        "Тостер", "Вафельница", "Фритюрница", "Су-вид",
        "Вок", "Казан", "Кухонные весы", "Сковорода", "Кастрюля"
    )
}

/**
 * Effort is distinct from elapsed cooking time. A recipe may take 2 hours
 * in the oven and have only 10 minutes of hands-on preparation.
 */
enum class CookingEffort(val storageId: String, val title: String, val description: String, val rank: Int) {
    FAST(
        "FAST", "Быстро пожрать",
        "Простые блюда, минимум движений и посуды.", 0
    ),
    INVOLVED(
        "INVOLVED", "Что-нибудь повыёбистее",
        "Можно повозиться: несколько техник и этапов.", 1
    ),
    ALL_OUT(
        "ALL_OUT", "Заебаться и угандошить всю посуду",
        "Сложные рецепты, много подготовки и гора посуды.", 2
    );

    companion object {
        fun fromStorageId(id: String?): CookingEffort =
            entries.firstOrNull { it.storageId == id } ?: FAST
    }
}

data class RecipePreparation(
    val requiredEquipment: Set<String>,
    val activeMinutes: Int,
    val totalMinutes: Int,
    val techniques: Int,
    val dishesToWash: Int
) {
    init {
        require(activeMinutes >= 0 && totalMinutes >= activeMinutes)
        require(techniques >= 0 && dishesToWash >= 0)
    }
}

/** Deterministic interim categorisation until M2 recipes have verified metadata. */
object RecipePreparationRules {
    fun classify(recipe: RecipePreparation): CookingEffort = when {
        recipe.activeMinutes <= 15 && recipe.techniques <= 2 && recipe.dishesToWash <= 2 ->
            CookingEffort.FAST
        recipe.activeMinutes <= 45 && recipe.techniques <= 4 && recipe.dishesToWash <= 5 ->
            CookingEffort.INVOLVED
        else -> CookingEffort.ALL_OUT
    }

    /** Cannot recommend a recipe using absent equipment or exceeding effort/time caps. */
    fun eligible(recipe: RecipePreparation, profile: FoodProfile): Boolean {
        val maxMinutes = profile.cookingMinutes.toIntOrNull() ?: return false
        return recipe.requiredEquipment.all { it in profile.appliances } &&
            recipe.totalMinutes <= maxMinutes &&
            classify(recipe).rank <= profile.cookingEffort.rank
    }
}
