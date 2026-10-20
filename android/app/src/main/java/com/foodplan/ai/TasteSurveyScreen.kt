package com.foodplan.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodplan.domain.TasteRating
import com.foodplan.domain.TasteSurvey

@Composable
fun TasteSurveyScreen(
    votes: Map<String, TasteRating>,
    onVote: (String, TasteRating) -> Unit,
    onClose: () -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    val dish = TasteSurvey.dishes[index]
    val choices = listOf(
        TasteRating.LIKE to "😍 Люблю",
        TasteRating.OK to "🙂 Нормально",
        TasteRating.DISLIKE to "Не нравится",
        TasteRating.NEVER_TRIED to "Не пробовал"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Пищевой портрет", style = MaterialTheme.typography.headlineMedium)
        Text("Блюдо ${index + 1} из 25 · оценено ${votes.size} из 25")
        LinearProgressIndicator(progress = { votes.size.coerceAtMost(25) / 25f },
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Text(dish.title, style = MaterialTheme.typography.headlineSmall)
        Text("Теги: ${dish.tags.sorted().joinToString(", ")}",
            style = MaterialTheme.typography.bodySmall)
        Text("Насколько тебе нравится это блюдо? Оценки сохраняются автоматически.")
        choices.forEach { (rating, label) ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onVote(dish.id, rating)
                    if (index < 24) index++ 
                }
            ) {
                Text(if (votes[dish.id] == rating) "✓ $label" else label)
            }
        }
        Spacer(Modifier.weight(1f))
        if (index == 24) Text(
            "Это последняя карточка. После выбора ответа можно вернуться к анкете.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (index > 0) index-- else onClose() }) {
                Text("Назад")
            }
            TextButton(onClick = onClose) { Text("В анкету") }
        }
    }
}
