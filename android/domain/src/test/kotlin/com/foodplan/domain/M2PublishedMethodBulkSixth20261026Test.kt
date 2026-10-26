package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/** YAR-6 sixth bulk: 25 examined, 12 source-documented candidates, 13 rejected. */
class M2PublishedMethodBulkSixth20261026Test {
    private data class Frozen(
        val id: String, val recipeSha: String, val excerptSha: String,
        val kcal: Double, val protein: Double, val fat: Double, val carbs: Double
    )
    // Frozen external source snapshots and raw-food arithmetic are not planner-generated.
    private val admitted = listOf(
        Frozen("b5_savory_millet_mushrooms", "62b35e2c1a2991cee37cce4d40330aef1e7af0a293941d21f2fe4faaa156c2d0", "e64ea27a8d82f118426fd58365dbcad9121193401519ab94c173820e64fe7321", 540.98, 23.68, 21.73, 64.9445),
        Frozen("b5_buckwheat_banana_casserole", "46b2d5880a6f9401d1594c022be09a6feda366e1c7a09109bbc854c6337a8e61", "16c143e554dc784f98040cee31a4a132252693bd9d9fedfa5ea5c837e03e6f9c", 578.55, 24.06, 14.951, 94.8385),
        Frozen("b5_apple_yogurt_layer_crepes", "a297d3e692112573c4fe9df49deb4336d7ec13c32919a03d07729c8467643da1", "02f26d9f2de0d815bd45d2f89db71b257de084948e0fc5a946915650a8c0dd2f", 665.84, 26.4775, 22.983, 90.9925),
        Frozen("b5_chicken_zucchini_boats_millet", "550a0ef5b84d572da3fac2948736b82e9389a7836da23dd69040c83fe9bf8fd2", "5aeab85db11f935b931e5f0a4f3de911e56fe12ca0b37a11e626870b46434bb5", 602.6912, 53.5655, 14.6192, 68.432),
        Frozen("b5_turkey_cabbage_oven_patties", "f8821157f127504b29341d1880afd1f8b6040162c108aff6b159abd6890b54be", "0d032f95be95b5bdf48aedbfb5066e833ff97cdccc424c034429b4e098fcd42c", 590.44675, 60.951125, 18.19375, 44.618875),
        Frozen("b5_turkey_broccoli_rice_milk_soup", "9983a50bc6ba2bda696a4715c72cdffd90748854c6f478c1219adf7ab59f4aa1", "0d8f85d5ae685a563b69d110e0e60e432161dbfc24cb984220af27e25e69e1be", 641.4435, 55.052695, 9.8671, 84.519205),
        Frozen("b5_beef_stuffed_mushroom_caps", "d1f4f64493ddbf6b0e9d2c17d0d647340348ac243b4531e3b4f6654d1c85b1ea", "f6bef634018b5b7e557df1fe07cbfc863d6d33b41b16911d1ef8985e0c7ed150", 657.63, 65.95, 37.478, 16.556),
        Frozen("b5_pork_millet_meatballs", "fa3d3737d997c4f858bd49af1455bd9e6f030dba7f90134fe9690e7a86282735", "83e8768a94fdf3f311fb1cf05d1b72838b8c2209f267d19a4da4186918292a06", 813.73, 52.9, 38.632, 62.4135),
        Frozen("b5_tofu_cabbage_tomato_pasta", "9d03c69668dcd927db4cc0a995dde1cadad6d3c89a87dbb91fa1f563696e89d6", "3d30cc5970f8cf9d6272559d8b26c62003fb3691713e4cf58bce5a1f2500dd15", 682.44775, 33.702625, 20.68975, 94.545875),
        Frozen("b5_chickpea_potato_gratin", "0e78db5240460689c22cd941f338321cecf24f882a663a1c54199dc0d31f7213", "96991c07e529889bb480bd8e737ff71f70ca8079929653c343fbd8fa1ef7bff2", 842.61, 36.4985, 30.5455, 110.948),
        Frozen("b5_lentil_cabbage_pepper_soup", "da601701589b96830ae0de065c6cae8fd801bfc7a498f3c8b250417633dd38ef", "244bb2b729e2a0fdb55f1f96301ba12d27435e5184051fe30f6e63958450ccaf", 564.685, 28.3914, 10.2326, 95.11),
        Frozen("b5_mushroom_zucchini_stuffed_potatoes", "838986a158d6c5ecb917ecdee2c556a7d5eafa2876ab34cca61299fcd9b1269e", "3bc97d9c183d5e60d57b76e481645b096c4eebbc002e8f920374821924d238d8", 629.72, 32.995, 28.538, 66.0865)
    )
    private val rejected = setOf(
        "b5_baked_tomato_eggs_cottage",
        "b5_chicken_pepper_skewers",
        "b5_beef_millet_tomato_bake",
        "b5_pork_apple_rice_skillet",
        "b5_cod_zucchini_roulades",
        "b5_cod_lentil_cucumber_salad",
        "b5_salmon_mushroom_rice_milk_soup",
        "b5_salmon_stuffed_peppers",
        "b5_tofu_broccoli_oat_cutlets",
        "b5_warm_chickpea_mushroom_broccoli_salad",
        "b4_egg_cucumber_buckwheat_breakfast",
        "b4_lentil_pepper_millet_stuff",
        "b4_salmon_millet_fish_soup"
    )

    @Test fun allTwentyFiveIndependentOutcomesAreUniqueAndFailClosed() {
        assertEquals(12, admitted.size)
        assertEquals(13, rejected.size)
        assertEquals(25, admitted.size + rejected.size)
        assertEquals(12, admitted.map { it.id }.toSet().size)
        assertTrue(admitted.none { it.id in rejected })
        assertEquals(12, M2PublishedMethodBulkSixth20261026.reviews.size)
        assertTrue(M2PublishedMethodBulkSixth20261026.reviews.keys.all { it.startsWith("b5_") })
        assertEquals(87, M2PublishedMethodReviews.reviews.size)
        for (id in rejected) {
            assertFalse("Sixth-batch rejected recipe: $id",
                M2PublishedMethodBulkSixth20261026.reviews.containsKey(id))
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == id }
            assertFalse(RecipeEvidenceAudit.inspect(recipe).documentedReady)
        }
    }

    @Test fun sourceRecipesAndIndependentRawKcalProteinFatCarbsMatch() {
        for (f in admitted) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == f.id }
            val review = M2PublishedMethodBulkSixth20261026.reviews.getValue(f.id)
            assertEquals(f.recipeSha, RecipeEvidenceAudit.fingerprint(recipe))
            assertEquals(f.recipeSha, review.recipeFingerprintSha256)
            assertEquals(f.excerptSha, review.methodSnapshotSha256)
            assertEquals(f.excerptSha,
                RecipeEvidenceAudit.sha256Utf8(requireNotNull(review.methodSnapshotText)))
            assertTrue(review.publishedMethodSource.startsWith("https://"))
            assertTrue(review.methodApplicabilityNotes.length > 30)
            assertEquals(RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE, review.nutritionBasis)
            val raw = requireNotNull(review.independentlyRecalculatedRawNutrition)
            val calculated = OfflineRecipes.nutrition(recipe)
            assertEquals(f.kcal, raw.kcal, 0.000001)
            assertEquals(f.protein, raw.protein, 0.000001)
            assertEquals(f.fat, raw.fat, 0.000001)
            assertEquals(f.carbs, raw.carbs, 0.000001)
            assertEquals(f.kcal, calculated.kcal, 0.00001)
            assertEquals(f.protein, calculated.protein, 0.00001)
            assertEquals(f.fat, calculated.fat, 0.00001)
            assertEquals(f.carbs, calculated.carbs, 0.00001)
            val inspection = RecipeEvidenceAudit.inspect(recipe, review)
            assertTrue("${f.id}: ${inspection.issues}", inspection.documentedReady)
            assertFalse(inspection.skuLabelCleared)
            assertFalse(inspection.physicalTrialDocumented)
            assertTrue(RecipeAuditNotice.FINISHED_YIELD_UNKNOWN in inspection.notices)
        }
    }

    @Test fun excerptManipulationAuthoredGramChangeAndFalseMacroInvalidateReview() {
        for (f in admitted) {
            val original = OfflineRecipes.curatedDrafts.single { it.id == f.id }
            val review = M2PublishedMethodBulkSixth20261026.reviews.getValue(f.id)
            val changedCapture = review.copy(
                methodSnapshotText = requireNotNull(review.methodSnapshotText) + " changed")
            assertTrue(RecipeAuditIssue.METHOD_SNAPSHOT_MISMATCHED in
                RecipeEvidenceAudit.inspect(original, changedCapture).issues)
            val changedRecipe = original.copy(ingredients = original.ingredients.mapIndexed { n, x ->
                if (n == 0) x.copy(grams = x.grams + 1) else x
            })
            assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE in
                RecipeEvidenceAudit.inspect(changedRecipe, review).issues)
            val macros = requireNotNull(review.independentlyRecalculatedRawNutrition)
            val fraudulent = review.copy(independentlyRecalculatedRawNutrition =
                macros.copy(kcal = macros.kcal + 9.0))
            assertTrue(RecipeAuditIssue.NUTRITION_RECALCULATION_MISMATCHED in
                RecipeEvidenceAudit.inspect(original, fraudulent).issues)
            assertFalse(RecipeEvidenceAudit.inspect(original, fraudulent).documentedReady)
        }
    }

    @Test fun documentaryM2StillOpenWithSixtyThreeUnauditedAuthoredDishes() {
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
    @Test fun tamperedDigestAloneRevokesEverySixthBatchReview() {
        for (frozen in admitted) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == frozen.id }
            val review = M2PublishedMethodBulkSixth20261026.reviews.getValue(frozen.id)
            val forged = review.copy(methodSnapshotSha256 = "0".repeat(64))
            val result = RecipeEvidenceAudit.inspect(recipe, forged)
            assertTrue("${frozen.id} local method SHA altered",
                RecipeAuditIssue.METHOD_SNAPSHOT_MISMATCHED in result.issues)
            assertFalse("${frozen.id} fail-closed", result.documentedReady)
        }
    }

}
