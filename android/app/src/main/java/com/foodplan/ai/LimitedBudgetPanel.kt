package com.foodplan.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodplan.domain.*
import kotlin.math.roundToInt

/** Partial affordable-food basket, never a balanced weekly ration. */
@Composable
fun LimitedBudgetPanel(
    profile: FoodProfile,
    store: String,
    offers: List<RetailerOffer>,
    manualPricesKopeks: Map<String, Long>,
    calorieTargetPerPerson: Int?
) {
    val basket = LimitedBudgetFoodPlanner.choose(profile,store,offers,
        manualPricesKopeks,System.currentTimeMillis(),calorieTargetPerPerson)
    HorizontalDivider(Modifier.padding(vertical=6.dp))
    Text("Что доступно на мой бюджет",
        style=MaterialTheme.typography.titleMedium)
    Text("Показываем даже неполную корзину, но не называем недостаток еды недельным рационом.",
        style=MaterialTheme.typography.bodySmall,
        color=MaterialTheme.colorScheme.error)
    when(basket.status) {
        LimitedFoodStatus.PARTIAL_AFFORDABLE_FOOD -> {
            Text("Оценка: ${ManualBasketPrices.formatKopeks(basket.spentKopeks)} ₽ из " +
                "${ManualBasketPrices.formatKopeks(basket.budgetKopeks)} ₽",
                style=MaterialTheme.typography.bodyLarge)
            basket.lines.forEach { item ->
                val source = if(item.origin==LimitedPriceOrigin.MANUALLY_ENTERED)
                    "цена вручную" else "наблюдение магазина"
                Text("• ${item.productName}: ${item.packages} × ${item.packageGrams} г, " +
                    "${ManualBasketPrices.formatKopeks(item.totalKopeks)} ₽ ($source)",
                    style=MaterialTheme.typography.bodyMedium)
            }
            val n=basket.totalNutritionEstimate
            Text("Из всех выбранных упаковок ~${n.kcal.roundToInt()} ккал, " +
                "белки ${n.protein.roundToInt()} г, " +
                "жиры ${n.fat.roundToInt()} г, " +
                "углеводы ${n.carbs.roundToInt()} г (до отходов и готовки).",
                style=MaterialTheme.typography.bodyMedium)
            basket.equivalentDaysByCalories?.let { days ->
                val length=if(days<1.0) "менее одного дня"
                    else "~${(days*10).roundToInt()/10.0} дня/дней"
                Text("Энергетический эквивалент для всего домохозяйства: $length. " +
                    "Это НЕ означает, что этими продуктами можно полноценно питаться столько времени.",
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.error)
            }
            basket.remainingWeekEnergyGapKcal?.let { gap ->
                if(gap>0) Text("По одной только энергии до расчётного ориентира на неделю " +
                    "не хватает ~${gap.roundToInt()} ккал. Белки, жиры, витамины и минералы также требуют оценки.",
                    color=MaterialTheme.colorScheme.error,
                    style=MaterialTheme.typography.bodyMedium)
            }
        }
        LimitedFoodStatus.NO_RELIABLE_PRICES -> {
            Text("Нет подходящих наблюдаемых или введённых вручную цен. " +
                "Нельзя обещать покупку за 100 ₽, если стоимость неизвестна.",
                style=MaterialTheme.typography.bodyMedium)
            if(profile.budgetRub=="100") {
                val potato=OfflineRecipes.ingredients.getValue("potato")
                Text("Условный пример, НЕ предложение магазина: если 2 кг картофеля стоят " +
                    "не более 100 ₽, это ~${(potato.kcal100*20).roundToInt()} ккал " +
                    "на всю покупку. Даже разделённые на три дня, это лишь " +
                    "~${(potato.kcal100*20/3).roundToInt()} ккал/день и крайне неполноценное питание.",
                    color=MaterialTheme.colorScheme.error,
                    style=MaterialTheme.typography.bodySmall)
            }
        }
        LimitedFoodStatus.NOTHING_AFFORDABLE ->
            Text("По известным ценам нет ни одной полной упаковки в этом бюджете. " +
                "Можно проверить другой магазин, добавить ручные цены или учесть домашние запасы.",
                style=MaterialTheme.typography.bodyMedium)
        LimitedFoodStatus.BLOCKED_SAFETY_RESTRICTIONS ->
            Text("Нельзя безопасно предложить даже частичный набор: " +
                "уточните ограничения или проверьте аллергенную маркировку.",
                color=MaterialTheme.colorScheme.error,
                style=MaterialTheme.typography.bodyMedium)
    }
    basket.warnings.forEach { message ->
        Text(message, color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.bodySmall)
    }
}
