package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class M2DocumentaryReviewQueueTest {
    @Test fun all150AuthoredDishesHaveImmutableFingerprintButNoFakeExternalProof() {
        val lines = M2DocumentaryReviewQueue.generateCsv().trimEnd().lines()
        assertEquals(151, lines.size)
        assertTrue(lines.first().contains("recipe_fingerprint_sha256"))
        assertTrue(lines.first().contains("captured_method_excerpt_utf8"))
        assertTrue(lines.first().contains("method_review_basis"))
        assertTrue(lines.first().contains("composed_published_step_count"))
        assertTrue(lines.first().contains("composed_published_step_source_links"))
        assertTrue(lines.first().contains("estimated_finished_yield_grams_optional"))
        assertTrue(lines.first().contains("physical_cooking_trial_reference_optional"))
        assertFalse(lines.first().contains("required_measured_cooked_yield"))
        val content = lines.drop(1).joinToString("\n")
        assertTrue(content.contains("PENDING_DOCUMENTARY_REVIEW"))
        assertTrue(content.contains("DOCUMENTARY_REVIEWED"))
        assertTrue(content.contains("https://1000.menu/cooking/50028-omlet-s-pomidorami-na-skovorode"))
        assertEquals(61,lines.drop(1).count { it.endsWith("\"PENDING_DOCUMENTARY_REVIEW\"") })
        assertEquals(89,lines.drop(1).count { it.endsWith("\"DOCUMENTARY_REVIEWED\"") })
        assertTrue(content.contains("RAW_EDIBLE_INPUT_ESTIMATE"))
        assertTrue(content.contains("COMPOSED_PUBLISHED_TECHNIQUES"))
        assertTrue(content.contains("https://www.quakeroats.com/cooking-and-recipes/carrot-cake-baked-oats"))
        assertTrue(content.contains("https://fdc.nal.usda.gov/food-details/"))
        assertTrue(content.contains("сырой") || content.contains("сухой"))
        // A queue with blank cookbook sources MUST NOT pass the actual review.
        assertEquals(89,RecipeEvidenceAudit.report().documentedReviewed)
        assertFalse(RecipeEvidenceAudit.report().meetsM2Gate)
    }

    @Test fun originalRecipeEditsChangeTheExternalReviewFingerprint() {
        val recipe = OfflineRecipes.curatedDrafts.first()
        val changed = recipe.copy(
            ingredients = recipe.ingredients.mapIndexed { i, v ->
                if (i == 0) v.copy(grams = v.grams + 2) else v
            }
        )
        assertNotEquals(RecipeEvidenceAudit.fingerprint(recipe),
            RecipeEvidenceAudit.fingerprint(changed))
        assertEquals(64, RecipeEvidenceAudit.fingerprint(recipe).length)
        assertEquals(RecipeEvidenceAudit.fingerprint(recipe),
            RecipeEvidenceAudit.fingerprint(recipe))
    }

    @Test fun generatedCombinatorialTemplatesCannotBeSubmittedAs150AuthoredDishes() {
        try {
            M2DocumentaryReviewQueue.generateCsv(
                OfflineRecipes.curatedDrafts.take(149) +
                    OfflineRecipes.generatedPrototypes.first())
            fail("Synthetic template must not masquerade as independently authored recipe")
        } catch (_: IllegalArgumentException) {
            // Expected hard rejection.
        }
    }
}
