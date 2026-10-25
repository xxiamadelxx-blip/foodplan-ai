package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class M2FourthBatchSourceTriageTest {
    @Test fun all25PrioritizedCandidatesAreUnacceptedAuthoredRecipes() {
        val rows = M2FourthBatchSourceTriage.remainingAgainst()
        assertEquals(25, rows.size)
        assertEquals((1..25).toList(), rows.map { it.priority })
        assertTrue(rows.all { it.recipeId !in M2PublishedMethodReviews.reviews })
        assertTrue(rows.all { it.blocker.isNotBlank() })
        assertEquals(1, rows.count { it.disposition ==
            M2FourthBatchSourceTriage.Disposition.METHOD_CANDIDATE })
        assertEquals(3, rows.count { it.disposition ==
            M2FourthBatchSourceTriage.Disposition.PARTIAL_METHOD_ONLY })
        assertEquals(1, rows.count { it.disposition ==
            M2FourthBatchSourceTriage.Disposition.SOURCE_NOT_APPLICABLE })
        assertEquals(20, rows.count { it.disposition ==
            M2FourthBatchSourceTriage.Disposition.UNSEARCHED })
    }

    @Test fun searchLeadsDoNotSilentlyPromoteM2AcceptedCount() {
        assertEquals(48, M2PublishedMethodReviews.reviews.size)
        assertEquals(48, RecipeEvidenceAudit.report().documentedReviewed)
        for (candidate in M2FourthBatchSourceTriage.candidates) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == candidate.recipeId }
            assertFalse(RecipeEvidenceAudit.inspect(recipe).documentedReady)
            assertFalse(RecipeEvidenceAudit.inspect(recipe).skuLabelCleared)
            if (candidate.publicationUrl != null) {
                assertTrue(candidate.publicationUrl.startsWith("https://"))
            }
        }
    }

    @Test fun acceptedCandidateDropsFromResearchQueueWithoutChangingHistory() {
        val first = M2FourthBatchSourceTriage.candidates.first().recipeId
        val fake = M2PublishedMethodReviews.reviews.values.first().copy(recipeId = first)
        val result = M2FourthBatchSourceTriage.remainingAgainst(
            accepted = M2PublishedMethodReviews.reviews + (first to fake))
        assertEquals(24, result.size)
        assertFalse(result.any { it.recipeId == first })
        assertEquals(25, M2FourthBatchSourceTriage.candidates.size)
    }
}
