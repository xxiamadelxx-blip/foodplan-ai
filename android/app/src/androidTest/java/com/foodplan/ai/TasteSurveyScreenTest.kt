package com.foodplan.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.foodplan.domain.TasteSurvey
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.performClick
import com.foodplan.domain.TasteRating
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TasteSurveyScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun cardAnswerUpdatesProgressAndAdvancesToNextDish() {
        val votes = mutableStateMapOf<String, TasteRating>()
        rule.setContent {
            MaterialTheme {
                TasteSurveyScreen(
                    votes = votes,
                    onVote = { id, vote -> votes[id] = vote },
                    onClose = {}
                )
            }
        }
        rule.onNodeWithText("Овсяная каша").assertExists()
        rule.onNodeWithText("😍 Люблю").performClick()
        rule.onNodeWithText("Омлет").assertExists()
        rule.runOnIdle {
            assertEquals(TasteRating.LIKE, votes["oatmeal"])
            assertEquals(1, votes.size)
        }
        rule.onNodeWithText("Блюдо 2 из 25 · оценено 1 из 25").assertExists()
    }

    @Test fun oldRestaurantVotesDoNotCountAsCompletedNewCards() {
        val votes = mutableStateMapOf("ramen" to TasteRating.DISLIKE)
        rule.setContent {
            MaterialTheme {
                TasteSurveyScreen(votes = votes,
                    onVote = { id, vote -> votes[id] = vote },
                    onClose = {})
            }
        }
        rule.onNodeWithText("Блюдо 1 из 25 · оценено 0 из 25").assertExists()
        rule.onNodeWithText("Овсяная каша").assertExists()
        rule.onNodeWithText("😍 Люблю").performClick()
        rule.onNodeWithText("Блюдо 2 из 25 · оценено 1 из 25").assertExists()
        rule.runOnIdle {
            assertEquals(TasteRating.DISLIKE, votes["ramen"])
            assertEquals(TasteRating.LIKE, votes["oatmeal"])
        }
    }

    @Test fun all25ImagesAreBundledDrawableResourcesWithAccessibleDescriptions() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(25, TasteDishArt.coveredDishIds().size)
        assertEquals(TasteSurvey.dishes.map { it.id }.toSet(),
            TasteDishArt.coveredDishIds())
        TasteSurvey.dishes.forEach { dish ->
            val art = TasteDishArt.resourceFor(dish.id)
            assertNotNull("No drawable for ${dish.id}", art)
            assertNotNull("Drawable cannot be decoded: ${dish.id}",
                target.resources.getDrawable(requireNotNull(art), target.theme))
        }
        assertNull(TasteDishArt.resourceFor("unknown_food"))
    }

    @Test fun all25ScreensHaveOfflineArtAndFourUsableAnswers() {
        val votes = mutableStateMapOf<String, TasteRating>()
        var completed = 0
        rule.setContent {
            MaterialTheme {
                TasteSurveyScreen(
                    votes = votes,
                    onVote = { id, rating -> votes[id] = rating },
                    onClose = {},
                    onCompleted = { completed++ }
                )
            }
        }
        TasteSurvey.dishes.forEachIndexed { index, dish ->
            rule.onNodeWithTag("taste-dish-image-${dish.id}").assertExists()
            rule.onNodeWithContentDescription(
                "Иллюстрация блюда: ${dish.title}").assertExists()
            rule.onNodeWithText(dish.title).assertExists()
            rule.onNodeWithText("Блюдо ${index+1} из 25 · оценено ${index} из 25")
                .assertExists()
            rule.onNodeWithText("😍 Люблю").assertExists()
            rule.onNodeWithText("🙂 Нормально").assertExists()
            rule.onNodeWithText("Не нравится").assertExists()
            rule.onNodeWithText("Не пробовал").performClick()
        }
        rule.runOnIdle {
            assertEquals(25, votes.size)
            assertTrue(votes.values.all { it == TasteRating.NEVER_TRIED })
            assertEquals(1, completed)
        }
    }

    @Test fun enlargedFontAndSmallViewportKeepAllAnswerTargetsAccessible() {
        val votes = mutableStateMapOf<String, TasteRating>()
        var completed = 0
        rule.setContent {
            MaterialTheme {
                // A deliberately tight 320x550 dp Android surface at 180%
                // system font scale. The survey cannot depend on a huge phone.
                val originalDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(originalDensity.density, fontScale = 1.8f)
                ) {
                    Box(Modifier.size(320.dp, 550.dp)) {
                        TasteSurveyScreen(
                            votes = votes,
                            onVote = { id, rating -> votes[id] = rating },
                            onClose = {},
                            onCompleted = { completed++ }
                        )
                    }
                }
            }
        }
        val labels = listOf("LIKE", "OK", "DISLIKE", "NEVER_TRIED")
        labels.forEach { rating ->
            rule.onNodeWithTag("taste-choice-$rating").assertIsDisplayed()
        }
        rule.onNodeWithText("Назад").assertIsDisplayed()
        rule.onNodeWithText("В приложение").assertIsDisplayed()
        rule.onNodeWithTag("taste-dish-image-oatmeal").assertExists()
        rule.onNodeWithContentDescription("Иллюстрация блюда: Овсяная каша")
            .assertExists()

        // The longest title and final-card progress should not push the
        // answers or navigation beyond the display at large text settings.
        repeat(24) {
            rule.onNodeWithTag("taste-choice-NEVER_TRIED").performClick()
        }
        rule.onNodeWithTag("taste-dish-image-vegetable_noodles_v2").assertExists()
        labels.forEach { rating ->
            rule.onNodeWithTag("taste-choice-$rating").assertIsDisplayed()
        }
        rule.onNodeWithText("Назад").assertIsDisplayed()
        rule.onNodeWithText("В приложение").assertIsDisplayed()
        rule.onNodeWithTag("taste-choice-LIKE").performClick()
        rule.runOnIdle {
            assertEquals(25, votes.size)
            assertEquals(TasteRating.LIKE, votes["vegetable_noodles_v2"])
            assertEquals(1, completed)
        }
    }

}
