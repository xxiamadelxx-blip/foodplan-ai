package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * 25-candidate fifth intake, pinned independently published methods.
 * Review of cooking technique and raw input calculations != cooked nutrition.
 */
class M2PublishedMethodBulkFifth20261025Test {
    private data class Frozen(
        val id: String, val authoredSha: String, val excerptSha: String,
        val kcal: Double, val protein: Double, val fat: Double, val carbs: Double
    )
    private val documented = listOf(
        Frozen("b4_rice_tomato_breakfast_cakes", "861d3d4815de838e618d01bd99afaac170ed6f72277d19922472b511e31a0b3a", "1c543f2e3957f69fae76145906713a6b0c98974f69459637990687e22a350a2b", 603.88825, 28.5322525, 28.94945, 58.3635475),
        Frozen("b4_millet_pear_pudding", "c09bd22f5b5446f80dbb539145ff81133bed4e7865b9d96d91497dedabd6958c", "fd7455f16e78d7254b8b233a0082a170a7167f90d23ac411b5498176ca2a6906", 548.858885, 21.673625, 15.950885, 79.08023),
        Frozen("b4_rolled_cottage_omelette", "bdd139bc947f2c5a8b30959a2219db7fd65cefddecc67ba33871bcb49222e1b2", "263dc30ef60e4c1d50ad419850b6fd5c5fdb054a70ca7d215cc590634947a03c", 412.38, 34.8145, 26.5225, 9.572),
        Frozen("b4_oat_crepes_pear", "07c395675282447e2eabf40e1c61055ceacdd675d4861d095abae65cf6cdcb84", "5de0d00f8e72acf91d63e26cf79840942aeddca9b9f3b72040e1133c2a264986", 593.708885, 23.106125, 22.442885, 75.71473),
        Frozen("b4_chicken_cabbage_noodle_soup", "47267d550de0e94a0013ce5b3093bcdfeea72d637bf07811d9543a66ef533679", "008fc5d393cfa497a4b552021b3374860d9621b5ea6019cbd93282f75e2a0270", 525.2901, 48.62225, 4.8076, 72.5085),
        Frozen("b4_chicken_broccoli_oat_balls", "db12b33eb9712ad7c8d84e3602e2e86efefb9cd704c515541d059380987f18b5", "bc10e5ab9f5540138e683bd96c18cad16b6cfe3b482528388111e05dbe308c3f", 522.9929, 57.35675, 18.4859, 36.126),
        Frozen("b4_turkey_mushroom_roulade", "65c1b341d735e191f9873252c14fbac9a931a3afd5c1d3458b05fe21240e1eff", "cdaf1154fe4c412c80a61b9795c9019435231c0637ed357dd9622d37744a1de8", 657.82, 65.9375, 23.866, 45.574),
        Frozen("b4_beef_zucchini_meatballs", "481e583a7ec58a8ba103ae5720a2837dd469f7b158603757e8b77ee2923159f2", "7c5b23533776ef973a9dc34d267d61e38453919deaafcee895012722c3b945db", 755.98, 60.3405, 26.121, 69.0145),
        Frozen("b4_pork_oat_schnitzel", "e80101df85f9731c35003467ce8fddc5b2cac87e3221ad9266db58abf45bd5a9", "76118851d1023348f1ecc81f73bfd708fd0b1c0d24e65b3bf827f53fd9ef1fed", 842.04125, 55.272, 47.83225, 45.91975)
    )
    private val rejected = setOf(
        "b5_buckwheat_banana_casserole",
        "b5_baked_tomato_eggs_cottage",
        "b5_savory_millet_mushrooms",
        "b4_savory_carrot_oat_bake",
        "b4_buckwheat_apple_cups",
        "b4_banana_cottage_oven_pancakes",
        "b4_egg_cucumber_buckwheat_breakfast",
        "b4_turkey_millet_cabbage_soup",
        "b4_beef_broccoli_patties",
        "b4_pork_potato_roast_lemon",
        "b4_cod_stuffed_tomatoes",
        "b4_cod_broccoli_yogurt_bake",
        "b4_salmon_millet_fish_soup",
        "b4_salmon_zucchini_oven_fritters",
        "b4_lentil_zucchini_cakes",
        "b4_lentil_pepper_millet_stuff"
    )

    @Test fun twentyFiveHistoricalOutcomesRemainUnchangedAfterLaterSources() {
        assertEquals(9, documented.size)
        assertEquals(16, rejected.size)
        assertEquals(25, documented.size + rejected.size)
        assertEquals(9, documented.map { it.id }.toSet().size)
        assertTrue(documented.none { it.id in rejected })
        assertEquals(9, M2PublishedMethodBulkFifth20261025.reviews.size)
        assertEquals(87, M2PublishedMethodReviews.reviews.size)
        for (id in rejected) {
            assertFalse("Fifth-batch rejected id: $id",
                M2PublishedMethodBulkFifth20261025.reviews.containsKey(id))
        }
    }

    @Test fun allNineMethodsPinFrozenAuthoredRecipeCaptureAndInputMacros() {
        for (row in documented) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == row.id }
            val review = M2PublishedMethodBulkFifth20261025.reviews.getValue(row.id)
            assertEquals(row.authoredSha, RecipeEvidenceAudit.fingerprint(recipe))
            assertEquals(row.authoredSha, review.recipeFingerprintSha256)
            assertEquals(row.excerptSha, review.methodSnapshotSha256)
            assertEquals(row.excerptSha, RecipeEvidenceAudit.sha256Utf8(
                requireNotNull(review.methodSnapshotText)))
            assertTrue(review.publishedMethodSource.startsWith("https://"))
            assertEquals(RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE, review.nutritionBasis)
            val fromInputs = OfflineRecipes.nutrition(recipe)
            val frozen = requireNotNull(review.independentlyRecalculatedRawNutrition)
            assertEquals(row.kcal, frozen.kcal, 0.000001)
            assertEquals(row.protein, frozen.protein, 0.000001)
            assertEquals(row.fat, frozen.fat, 0.000001)
            assertEquals(row.carbs, frozen.carbs, 0.000001)
            assertEquals(row.kcal, fromInputs.kcal, 0.00001)
            assertEquals(row.protein, fromInputs.protein, 0.00001)
            assertEquals(row.fat, fromInputs.fat, 0.00001)
            assertEquals(row.carbs, fromInputs.carbs, 0.00001)
            val status = RecipeEvidenceAudit.inspect(recipe, review)
            assertTrue("${row.id}: ${status.issues}", status.documentedReady)
            assertFalse(status.skuLabelCleared)
            assertFalse(status.physicalTrialDocumented)
            assertTrue(RecipeAuditNotice.FINISHED_YIELD_UNKNOWN in status.notices)
        }
    }

    @Test fun changingCapturedMethodOriginalGramsOrMacronutrientsRevokesProof() {
        for (row in documented) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == row.id }
            val review = M2PublishedMethodBulkFifth20261025.reviews.getValue(row.id)
            val alteredText = review.copy(
                methodSnapshotText = requireNotNull(review.methodSnapshotText) + "untrusted change")
            assertTrue(RecipeAuditIssue.METHOD_SNAPSHOT_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, alteredText).issues)
            val editedRecipe = recipe.copy(ingredients = recipe.ingredients.mapIndexed { i, v ->
                if (i == 0) v.copy(grams = v.grams + 2) else v
            })
            assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE in
                RecipeEvidenceAudit.inspect(editedRecipe, review).issues)
            val macros = requireNotNull(review.independentlyRecalculatedRawNutrition)
            val adulterated = review.copy(independentlyRecalculatedRawNutrition =
                macros.copy(kcal = macros.kcal + 10.0))
            assertTrue(RecipeAuditIssue.NUTRITION_RECALCULATION_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, adulterated).issues)
            assertFalse(RecipeEvidenceAudit.inspect(recipe, adulterated).documentedReady)
        }
    }

    @Test fun completeM2RemainsOpenAtSeventyFiveOfOneHundredFifty() {
        val report = RecipeEvidenceAudit.report()
        assertEquals(150, report.distinctDrafts)
        assertEquals(200, report.generatedTemplates)
        assertEquals(87, report.documentedReviewed)
        assertEquals(63, OfflineRecipes.curatedDrafts.count {
            it.id !in M2PublishedMethodReviews.reviews
        })
        assertEquals(0, report.skuLabelsClearedByRecipes)
        assertFalse(report.meetsM2Gate)
    }
}
