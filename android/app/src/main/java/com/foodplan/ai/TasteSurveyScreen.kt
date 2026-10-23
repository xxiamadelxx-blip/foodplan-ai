package com.foodplan.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.foodplan.domain.TasteRating
import com.foodplan.domain.TasteSurvey

/**
 * No image request or AI call at runtime. Food illustration is packaged in
 * drawable-nodpi; title and the four choices remain usable without images.
 */
@Composable
fun TasteSurveyScreen(
    votes: Map<String, TasteRating>,
    onVote: (String, TasteRating) -> Unit,
    onClose: () -> Unit,
    onCompleted: () -> Unit = onClose
) {
    var index by remember { mutableIntStateOf(0) }
    val dish = TasteSurvey.dishes[index]
    val answered = TasteSurvey.migrateVotes(votes).size
    val choices = listOf(
        TasteRating.LIKE to "😍 Люблю",
        TasteRating.OK to "🙂 Нормально",
        TasteRating.DISLIKE to "Не нравится",
        TasteRating.NEVER_TRIED to "Не пробовал"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // At Android's enlarged text scales, keep the four vote targets
        // and both navigation actions on-screen. Illustration and prompt
        // scroll independently rather than pushing buttons off the device.
        val enlargedFont = LocalDensity.current.fontScale >= 1.3f
        val compact = maxHeight < 660.dp || enlargedFont
        Column(
            modifier = Modifier.fillMaxSize().padding(
                horizontal = if (compact) 10.dp else 16.dp,
                vertical = if (compact) 4.dp else 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
        ) {
            Text(
                "Пищевой портрет",
                style = if (compact) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.headlineMedium
            )
            Text(
                if (enlargedFont)
                    "Блюдо ${index + 1}/25 · Оценено ${answered}/25"
                else "Блюдо ${index + 1} из 25 · оценено ${answered} из 25",
                style = if (compact) MaterialTheme.typography.labelMedium
                    else MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { answered.coerceAtMost(25) / 25f },
                modifier = Modifier.fillMaxWidth()
            )

        // Only the illustration/description area scrolls. All four answer
        // buttons and the navigation remain visible on compact phone screens.
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val illustration = TasteDishArt.resourceFor(dish.id)
                if (illustration != null) {
                    Image(
                        painter = painterResource(illustration),
                        contentDescription = "Иллюстрация блюда: ${dish.title}",
                        modifier = Modifier.fillMaxWidth()
                            .height(if (compact) 136.dp else 188.dp)
                            .testTag("taste-dish-image-${dish.id}"),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Defensive fallback if a future catalog adds a dish
                    // without an image. The user still has an operable survey.
                    Text(
                        "Изображение не добавлено",
                        modifier = Modifier.testTag("taste-dish-image-fallback"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(dish.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Насколько тебе нравится это блюдо? Оценки сохраняются автоматически.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Two-row grid is shorter than four full-width buttons and keeps all
        // answers accessible without scrolling even with a larger system font.
        choices.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (rating, label) ->
                    OutlinedButton(
                        modifier = Modifier.weight(1f)
                            .heightIn(min = if (compact) 48.dp else 56.dp)
                            .testTag("taste-choice-${rating.name}"),
                        onClick = {
                            onVote(dish.id, rating)
                            if (index < TasteSurvey.dishes.lastIndex) index++
                            else onCompleted()
                        }
                    ) {
                        Text(if (votes[dish.id] == rating) "✓ $label" else label)
                    }
                }
            }
        }

        // The optional final-card explanation does not consume the fixed
        // action area at very large font scales.
        if (index == TasteSurvey.dishes.lastIndex && !compact) {
            Text(
                "Это последняя карточка. После оценки откроется приложение.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { if (index > 0) index-- else onClose() }) {
                Text("Назад")
            }
            TextButton(onClick = onClose) { Text("В приложение") }
        }
        }
    }
}
