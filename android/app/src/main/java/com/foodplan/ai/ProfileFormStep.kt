package com.foodplan.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.foodplan.domain.WeeklyBudget
import com.foodplan.domain.FoodProfile
import com.foodplan.domain.CookingEffort
import com.foodplan.domain.KitchenEquipment

private val stores = listOf(
    "Пятёрочка", "Магнит", "Лента", "Перекрёсток", "Дикси", "О’КЕЙ",
    "ВкусВилл", "Ашан", "Самокат", "Яндекс Лавка", "Ozon fresh", "Другой"
)

@Composable
fun ProfileFormStep(step: Int, p: FoodProfile, change: (FoodProfile) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (step) {
            0 -> {
                Section("О вас", "Эти данные нужны только для будущего расчёта рациона.")
                OneChoice("Пол", listOf("Женский","Мужской","Не указывать"), p.sex) {
                    change(p.copy(sex = it))
                }
                Field("Возраст, лет", p.age, true) { change(p.copy(age = it)) }
                Field("Рост, см", p.heightCm, true) { change(p.copy(heightCm = it)) }
                Field("Вес, кг", p.weightKg, true) { change(p.copy(weightKg = it)) }
                OneChoice("Активность", listOf("Низкая","Умеренная","Высокая","Очень высокая"),
                    p.activity) { change(p.copy(activity = it)) }
            }
            1 -> {
                Section("Цель питания", "Это выбор направления, не медицинское назначение.")
                OneChoice("Моя цель", listOf("Суперсушка","Похудение","Поддержание","Набор массы"),
                    p.goal) { change(p.copy(goal = it)) }
                Text("Режим «Суперсушка» не разрешает опасное ограничение калорий. Для несовершеннолетних изменение веса не рассчитывается автоматически.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            2 -> {
                Section("Бюджет и магазины", "Рассчитываем рубли и полную стоимость упаковок.")
                Field("Город России", p.city) { change(p.copy(city = it)) }
                ExactBudgetSlider(p.budgetRub) { change(p.copy(budgetRub = it)) }
                MultiChoice("Где обычно покупаете?", stores, p.stores) { store ->
                    change(p.copy(stores = p.stores.toggle(store)))
                }
                Text("Выбор магазина пока не означает наличие интеграции с его ценами.",
                    style = MaterialTheme.typography.bodySmall)
            }
            3 -> {
                Section("Тип питания и ограничения", "Аллергии отличаются от продуктов, которые просто не нравятся.")
                OneChoice("Тип питания", listOf("Всеядный", "Вегетарианский", "Веганский",
                    "Пескетарианский"), p.diet) { change(p.copy(diet = it)) }
                Field("Аллергены (через запятую)", p.allergens) {
                    change(p.copy(allergens = it))
                }
                Field("Не люблю (через запятую)", p.disliked) {
                    change(p.copy(disliked = it))
                }
                Text("Аллергены являются строгим запретом. При неизвестном составе блюдо не будет признано безопасным.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
            4 -> {
                Section("Привычки и кухня", "Настроим порции и удобство готовки.")
                Field("На сколько человек", p.household, true) {
                    change(p.copy(household = it))
                }
                Field("Приёмов пищи в день", p.mealsPerDay, true) {
                    change(p.copy(mealsPerDay = it))
                }
                Field("Максимум минут на приготовление", p.cookingMinutes, true) {
                    change(p.copy(cookingMinutes = it))
                }
                CookingEffortChoice(p.cookingEffort) { level ->
                    change(p.copy(cookingEffort = level))
                }
                MultiChoice("Что есть на кухне?", KitchenEquipment.choices, p.appliances) {
                    change(p.copy(appliances = p.appliances.toggle(it)))
                }
                Text("После сохранения данные остаются на телефоне. Меню на 7 дней появится на следующих этапах разработки.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value

@Composable
private fun Section(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(description, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Field(label: String, value: String, numeric: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text
        )
    )
}

/** Continuous gesture + exact numeric input, including 356 and 63 218 ₽. */
@Composable
private fun ExactBudgetSlider(value: String, onChange: (String) -> Unit) {
    val amount = WeeklyBudget.parse(value)
    Text("Бюджет на 7 дней", style = MaterialTheme.typography.titleMedium)
    Text("${amount ?: "—"} ₽", style = MaterialTheme.typography.headlineMedium)
    Slider(
        value = (value.toFloatOrNull() ?: WeeklyBudget.MIN_RUB.toFloat())
            .coerceIn(WeeklyBudget.MIN_RUB.toFloat(),WeeklyBudget.MAX_RUB.toFloat()),
        onValueChange = { onChange(it.roundToInt()
            .coerceIn(WeeklyBudget.MIN_RUB,WeeklyBudget.MAX_RUB).toString()) },
        valueRange = WeeklyBudget.MIN_RUB.toFloat()..WeeklyBudget.MAX_RUB.toFloat(),
        modifier = Modifier.fillMaxWidth().testTag("weekly-budget-slider")
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("100 ₽", style = MaterialTheme.typography.labelSmall)
        Text("100 000 ₽", style = MaterialTheme.typography.labelSmall)
    }
    OutlinedTextField(
        value = value,
        onValueChange = { entered ->
            if (entered.isEmpty() || entered.length <= 6 && entered.all(Char::isDigit))
                onChange(entered)
        },
        label = { Text("Точно до рубля: например 356 или 63218") },
        modifier = Modifier.fillMaxWidth().testTag("weekly-budget-exact-input"), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = value.isNotEmpty() && amount == null
    )
    Text(
        "Маленький бюджет не скрывает результат: неполная корзина будет показана с предупреждением о дефиците питания и непроверенных ценах.",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun OneChoice(label: String, options: List<String>, selection: String,
                      onChoice: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    options.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { item ->
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = selection == item, onClick = { onChoice(item) },
                    label = { Text(item) }
                )
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun MultiChoice(label: String, options: List<String>, chosen: Set<String>,
                        onToggle: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    options.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { item ->
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = item in chosen, onClick = { onToggle(item) },
                    label = { Text(item) }
                )
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** Full-width rows deliberately avoid truncating the third, long option. */
@Composable
private fun CookingEffortChoice(selected: CookingEffort, onChoice: (CookingEffort) -> Unit) {
    Text("Насколько хочешь заморочиться с рецептами?",
        style = MaterialTheme.typography.titleMedium)
    Text("Это сложность и количество грязной посуды. Лимит времени задаётся отдельно.",
        style = MaterialTheme.typography.bodySmall)
    CookingEffort.entries.forEach { level ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChoice(level) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected == level,
                onClick = { onChoice(level) }
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(level.title, style = MaterialTheme.typography.bodyLarge)
                Text(level.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
