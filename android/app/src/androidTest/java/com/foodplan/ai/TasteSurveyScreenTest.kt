package com.foodplan.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
        rule.onNodeWithText("Борщ").assertExists()
        rule.onNodeWithText("😍 Люблю").performClick()
        rule.onNodeWithText("Гречка с курицей").assertExists()
        rule.runOnIdle {
            assertEquals(TasteRating.LIKE, votes["borsch"])
            assertEquals(1, votes.size)
        }
        rule.onNodeWithText("Блюдо 2 из 25 · оценено 1 из 25").assertExists()
    }
}
