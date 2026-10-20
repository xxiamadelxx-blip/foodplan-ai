package com.foodplan.domain

data class FoodProfile(
    val sex: String = "Не указывать",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val activity: String = "Умеренная",
    val goal: String = "Поддержание",
    val city: String = "Санкт-Петербург",
    val budgetRub: String = "",
    val stores: Set<String> = setOf("Пятёрочка"),
    val diet: String = "Всеядный",
    val allergens: String = "",
    val disliked: String = "",
    val household: String = "1",
    val mealsPerDay: String = "3",
    val cookingMinutes: String = "45",
    val appliances: Set<String> = setOf("Плита")
)

/** Strict validation for the onboarding form; NOT nutrition or medical advice. */
object ProfileValidator {
    private fun positiveInt(s: String, range: IntRange): Boolean =
        s.toIntOrNull()?.let { it in range } == true

    fun errors(p: FoodProfile): List<String> = buildList {
        if (!positiveInt(p.age, 1..120)) add("Возраст: введите число от 1 до 120.")
        if (!positiveInt(p.heightCm, 80..250)) add("Рост: введите 80–250 см.")
        val weight = p.weightKg.replace(',', '.').toDoubleOrNull()
        if (weight == null || !weight.isFinite() || weight !in 15.0..400.0)
            add("Вес: введите число от 15 до 400 кг.")
        val budget = p.budgetRub.toLongOrNull()
        if (budget == null || budget !in 1L..10_000_000L)
            add("Бюджет: введите целую сумму от 1 до 10 000 000 ₽.")
        if (p.city.trim().length < 2 || p.city.length > 120) add("Укажите город России.")
        if (p.stores.isEmpty()) add("Выберите хотя бы один магазин.")
        if (!positiveInt(p.household, 1..20)) add("Количество человек: от 1 до 20.")
        if (!positiveInt(p.mealsPerDay, 1..6)) add("Приёмов пищи: от 1 до 6.")
        if (!positiveInt(p.cookingMinutes, 5..240)) add("Время готовки: от 5 до 240 минут.")
        if (p.age.toIntOrNull()?.let { it < 18 } == true &&
            p.goal in setOf("Суперсушка", "Похудение", "Набор массы"))
            add("Для несовершеннолетних цели изменения веса не рассчитываются без медицинского сопровождения.")
    }
}
