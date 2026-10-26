package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/** Gate: research leads do not equal independently documented published methods. */
class M2FourthBatchSourceTriageTest {
    @Test fun acceptedMethodsLeaveFourteenDistinctPendingFromOriginalResearchBatch() {
        val rows = M2FourthBatchSourceTriage.remainingAgainst()
        assertEquals(14, rows.size)
        assertEquals(rows.map { it.priority }.sorted(), rows.map { it.priority })
        assertTrue(rows.all { it.recipeId !in M2PublishedMethodReviews.reviews })
        assertTrue(rows.all { it.blocker.isNotBlank() })
        assertEquals(0, rows.count { it.disposition ==
            M2FourthBatchSourceTriage.Disposition.METHOD_CANDIDATE })
        assertEquals(0, rows.count { it.disposition ==
            M2FourthBatchSourceTriage.Disposition.PARTIAL_METHOD_ONLY })
        assertEquals(1, rows.count { it.disposition ==
            M2FourthBatchSourceTriage.Disposition.SOURCE_NOT_APPLICABLE })
        assertEquals(13, rows.count { it.disposition ==
            M2FourthBatchSourceTriage.Disposition.UNSEARCHED })
    }

    @Test fun searchLeadsDoNotSilentlyPromoteM2AcceptedCount() {
        assertEquals(87, M2PublishedMethodReviews.reviews.size)
        assertEquals(87, RecipeEvidenceAudit.report().documentedReviewed)
        for (candidate in M2FourthBatchSourceTriage.candidates) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == candidate.recipeId }
            assertEquals(
                candidate.recipeId in M2PublishedMethodReviews.reviews,
                RecipeEvidenceAudit.inspect(
                    recipe, M2PublishedMethodReviews.reviews[candidate.recipeId]
                ).documentedReady
            )
            assertFalse(RecipeEvidenceAudit.inspect(recipe).skuLabelCleared)
            if (candidate.publicationUrl != null) {
                assertTrue(candidate.publicationUrl.startsWith("https://"))
            }
        }
    }

    @Test fun acceptedCandidateDropsFromResearchQueueWithoutChangingHistory() {
        val first = M2FourthBatchSourceTriage.remainingAgainst().first().recipeId
        val fake = M2PublishedMethodReviews.reviews.values.first().copy(recipeId = first)
        val result = M2FourthBatchSourceTriage.remainingAgainst(
            accepted = M2PublishedMethodReviews.reviews + (first to fake))
        assertEquals(13, result.size)
        assertFalse(result.any { it.recipeId == first })
        assertEquals(25, M2FourthBatchSourceTriage.candidates.size)
    }
}
