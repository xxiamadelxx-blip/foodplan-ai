package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/** One auditable 20-candidate batch: 17 reviewed, 3 rejected. */
class M2PublishedMethodBulk20261024Test {
    private val acceptedIds = listOf("cur_syrniki", "cur_tofu_veg_rice", "cur_chicken_mushroom_pasta", "cur_beef_mushroom_stew", "cur_pork_cabbage", "cur_vegetable_chickpea_stew", "cur_zucchini_fritters", "cur_broccoli_egg_bake", "cur_chicken_cabbage_stew", "cur_chicken_pilaf_carrot", "cur_egg_cabbage_skilet", "cur_potato_broccoli_soup", "cur_chickpea_pasta", "cur_lentil_vegetable_curry", "cur_tomato_lentil_pasta", "cur_oven_chicken_vegetables", "cur_apple_cottage_cheese_bake")
    private val rejectedIds = setOf("cur_turkey_stuffed_peppers",
        "cur_baked_potato_chickpeas", "cur_tomato_baked_eggs_cheese")

    // Frozen external research result, independent of the planner or the
    // production registry. Wrong edited source content must fail closed.
    private val frozenEvidence = listOf(
        Triple("cur_syrniki", "46202fa64ec61ec605dca18d98dc575c575aa551f84767ac599baecd89da5d54", "8a40f6019b4b7913547427b06e71cb9f875903946a64fd789fdfa98d5a070b9d"),
        Triple("cur_tofu_veg_rice", "0c1c342e168935ea67f95e77c06cd61d6229a46de709b389d21ab7f4a7061f6f", "8d56582a8fecd21bb1d5b6bfa98817a3d1a88302d484bbc803633f361bc65d7b"),
        Triple("cur_chicken_mushroom_pasta", "dc51a28ac49aa5bca0e953aa3e6b5e4db7f08fa9d74938d01385232ab3dc3547", "1566f46ff90f80a30f4ff603fe22493800841025c15773627b48ccf857bd074d"),
        Triple("cur_beef_mushroom_stew", "f2b42e758bab20e952c0d83789cff7a147644e1838622a51b5c2e215079fab25", "2d2169b8a3ed90057b61c3f646a737d51b509054168a730c2605cf81695a0b4b"),
        Triple("cur_pork_cabbage", "190976998d5d3b5bb64751941d76297faee91d229a218a4b9b7527f293353d71", "4341eb0dc549e9ead0e46494fcacb430b669da9769820347691eeb9fa94b2bbf"),
        Triple("cur_vegetable_chickpea_stew", "e999ab544e342b462602e972220bfdcdd90eae7756dfcded3b6fd1ac65c17b07", "52826cf18f4c5e6f341f1c2d6542f3616e45355288df4bc04c631cdab187dbcf"),
        Triple("cur_zucchini_fritters", "7566f16e7f72d5550ee05ef4444206193359477df3dcc8de683464caeab85045", "b17097a2f725b257bd1dd87eb1f8acaf22c908e77369fa8da33cfb8803f9dd7b"),
        Triple("cur_broccoli_egg_bake", "fc7608316fa953bddf27d36741f59a95b2916e0f719ed83a01ec82ead9b56d20", "0db7e166acd4c8bff4c182cf8cbe93f129a47bb6b46ed103e4f2ed47161cb179"),
        Triple("cur_chicken_cabbage_stew", "3081ae5c3e740eb782838ba98f6998e0681cc1cf44c84e2ed0cebabddd6e9f92", "c85b4578917be8cf198bba0c917fed2b83bdd4b9a4601f1fa125b15631e3af37"),
        Triple("cur_chicken_pilaf_carrot", "28eccb12e29dcca4de99ecce6f3506d22fce583aa18c55619c376dc931aac22f", "a9c938df4bbd0893f6fb85a64d4b0d9cb6cd4f12da4f8a65f98e766331a78adb"),
        Triple("cur_egg_cabbage_skilet", "4322f2697eddecbc5576b453be0e320b55390041dbf6c26c2a06cc6db900cd1d", "098c1fbc22f5b2396f5d3d09f025fb7ad00422043bcd6585baa918de0f3ef06e"),
        Triple("cur_potato_broccoli_soup", "dde6202e44f95cdeed670f1491d7d3e03f7ba08c2d5fc6539e42c94005f66d41", "fcd23761184a68a9aefbe4b08e7ec2ff7db78720a89edf13eb00fdb197152c08"),
        Triple("cur_chickpea_pasta", "c583285df254fc883275e5bc80c10013f488748d9f4fbce78a7700efeb45bf7d", "c2f34fddfda84751f98992737cfe3049e9edd9c76f8ddbc1763718cb01003534"),
        Triple("cur_lentil_vegetable_curry", "cb4481dece66d363e35d84f33fbc3fdf96dfb9545398d5ddf5d40be597fde2a7", "20d201568f0122ae25f4f8ef0568e901cb70ccfe16ed1c7e5560692a55563f93"),
        Triple("cur_tomato_lentil_pasta", "7adf1f7eb2043b0927a96d466c297cb1005ea9db6038645296e0b0b84874c8a2", "612d189c123413da39a5c5a17745a5ea7137c508438b56dbc13d8fdd3d0354b5"),
        Triple("cur_oven_chicken_vegetables", "2c2ce7b0f2424331e019ce4887d13b9c6a91d578d439d355ec50e7219e3599f0", "dc7a78c9ef052d91da39bbde867effd8a27c0bc8aa05120d29b57f04b84cfdbc"),
        Triple("cur_apple_cottage_cheese_bake", "6c098d9fee7c498a74dfa5931928d85edfb2da362e8b9ef4cb5334e838320cc0", "51450ea94cb71492a8ab267f5422ebbe0ad8a6c5f79c0b7d6d786367f98367e2")
    )

    @Test fun bulkIntakeReviewsSeventeenDistinctRealMethodsWithoutPromotingRejected() {
        assertEquals(17, acceptedIds.distinct().size)
        assertEquals(17, M2PublishedMethodBulk20261024.reviews.size)
        assertEquals(48,M2PublishedMethodReviews.reviews.size)
        assertTrue(rejectedIds.intersect(M2PublishedMethodReviews.reviews.keys).isEmpty())
        assertEquals(17, frozenEvidence.size)
        for ((id, recipeSha, capturedSha) in frozenEvidence) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == id }
            val evidence = M2PublishedMethodReviews.reviews.getValue(id)
            assertEquals(id, evidence.recipeId)
            assertEquals(recipeSha, RecipeEvidenceAudit.fingerprint(recipe))
            assertEquals(recipeSha, evidence.recipeFingerprintSha256)
            assertEquals(capturedSha, evidence.methodSnapshotSha256)
            assertEquals(capturedSha,
                RecipeEvidenceAudit.sha256Utf8(requireNotNull(evidence.methodSnapshotText)))
            assertTrue(evidence.publishedMethodSource.startsWith("https://"))
            assertEquals(RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE, evidence.nutritionBasis)
            val actual = OfflineRecipes.nutrition(recipe)
            val separatelyTabulated = requireNotNull(evidence.independentlyRecalculatedRawNutrition)
            assertEquals(actual.kcal, separatelyTabulated.kcal, 0.00001)
            assertEquals(actual.protein, separatelyTabulated.protein, 0.00001)
            assertEquals(actual.fat, separatelyTabulated.fat, 0.00001)
            assertEquals(actual.carbs, separatelyTabulated.carbs, 0.00001)
            val audit = RecipeEvidenceAudit.inspect(recipe, evidence)
            assertTrue("Bulk reviewed ID " + id + ": " + audit.issues, audit.documentedReady)
            assertFalse(audit.physicalTrialDocumented)
            assertFalse(audit.skuLabelCleared)
            assertTrue(RecipeAuditNotice.FINISHED_YIELD_UNKNOWN in audit.notices)
            assertTrue(RecipeAuditNotice.RAW_INPUT_NUTRITION_APPROXIMATE in audit.notices)
        }
    }

    @Test fun eachOfSeventeenRejectsTamperedSourceNutritionAndRecipe() {
        for (id in acceptedIds) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == id }
            val evidence = M2PublishedMethodReviews.reviews.getValue(id)
            val tamperedExcerpt = evidence.copy(methodSnapshotText =
                requireNotNull(evidence.methodSnapshotText) + "TAMPER")
            assertTrue(RecipeAuditIssue.METHOD_SNAPSHOT_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, tamperedExcerpt).issues)
            val stale = recipe.copy(ingredients =
                recipe.ingredients.mapIndexed { index, ingredient ->
                    if (index == 0) ingredient.copy(grams = ingredient.grams + 1)
                    else ingredient
                })
            assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE in
                RecipeEvidenceAudit.inspect(stale, evidence).issues)
            val numbers = requireNotNull(evidence.independentlyRecalculatedRawNutrition)
            val falseValues = evidence.copy(independentlyRecalculatedRawNutrition =
                numbers.copy(protein = numbers.protein + 9.0))
            assertTrue(RecipeAuditIssue.NUTRITION_RECALCULATION_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, falseValues).issues)
        }
    }

    @Test fun remainingOneHundredTwentyFiveAuthoredMethodsAreStillNotApproved() {
        val report = RecipeEvidenceAudit.report()
        assertEquals(150, report.distinctDrafts)
        assertEquals(200, report.generatedTemplates)
        assertEquals(48,report.documentedReviewed)
        assertEquals(102, OfflineRecipes.curatedDrafts.count {
            it.id !in M2PublishedMethodReviews.reviews
        })
        assertFalse(report.meetsM2Gate)
    }
}
