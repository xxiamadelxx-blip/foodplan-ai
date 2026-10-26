package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * Regression for 25 independent YAR-6 M2 candidates: 18 method-evidenced,
 * 7 not admitted. Literal expected data was pinned from a separate author
 * review (docs/qa/M2_BULK25_FOURTH_20261025_EVIDENCE.json).
 * Never recalculate fixture values by calling the production planner.
 */
class M2PublishedMethodBulkFourth20261025Test {
    private data class Frozen(
        val id: String, val recipeSha: String, val excerptSha: String,
        val publishedSource: String,
        val kcal: Double, val protein: Double, val fat: Double, val carbs: Double
    )
    private val frozen = listOf(
        Frozen("cur_zucchini_cheese_omelette", "8a0864cb15f876ed0dbcc2571b99f6712d20d14a144d79ca4b5cf31606d8a48f", "7617294e4793556b1c81c0a504466fac55965039d7a6423f8451cebf6cec9a23", "https://www.epicurious.com/recipes/food/views/herbed-cheese-and-zucchini-omelet-479", 419.19, 28.659, 31.31, 7.175),
        Frozen("cur_chicken_rice_soup", "8fdb2e7680370d4b4508e7bb3ca37d7870fcb037efec5bc5c05ab1895affccd2", "334d4dcd348864ad7c37909e9e03a8934693aee06c8cbcbb91b862c8aa425298", "https://kamelena.com/en/recipe/Chicken-Soup-with-Rice", 558.0257, 43.75556, 4.0405, 89.12789),
        Frozen("cur_broccoli_cheese_puree", "1664e7b2ddcff2907a921d28b76da9d27dd5b49ef3cff51b3db2cba9e1210d06", "d4b08660d164691c997b741c3d2ee5e08521df26c4f3763d8931bba94a00add8", "https://www.arlafoods.co.uk/recipes/broccoli-and-potato-soup/", 518.28, 25.832, 23.339, 57.644),
        Frozen("cur_beef_chickpea_braise", "1693d8f01b5ee981b53370822f87e217dab99b98daeaba43d57f8f5baf76c1f4", "18bcf0c0cf9522897b24ae38be268ba52b9478b336f6ff80d1de9851b4c9094d", "https://akispetretzikis.com/en/recipe/8843/moscharaki-kokkinisto-me-revithia", 718.2, 58.528, 27.915, 60.074),
        Frozen("cur_vegetable_chickpea_pilaf", "82da630f0a60d02ef647c2f068e87191f18461d8a1a1823549701b14e9ddce89", "2cd1f0c1247cba74a01a149838099c026e144e8e33296d15bec1341281255a69", "https://tollbit.bbcgoodfood.com/recipes/1959653/spiced-carrot-chickpea-and-almond-pilaf", 803.9545, 22.240965, 19.6567, 139.885835),
        Frozen("cur_tofu_tomato_pasta", "ed28b42c1d5c440c3ae3c12c68f638026142323116c6475ed82ba83bfe3b56f2", "297b93b77fc9477d1565452667ba074dd5d6fcbaa218cb9c6da00c45abd09d7c", "https://khinskitchen.com/tofu-pasta/", 625.76, 31.2864, 19.7181, 85.0985),
        Frozen("cur_pork_pepper_wok", "6c6fbb7b40541796f8ec259c6cc56508da2394e3d5c6f9ecec536d84d2e49d86", "4df655be7bf8a4cdb3ebcd43c897001ff99ab721d97af16ad788c4375e4c1462", "https://www.foodreference.com/html/sw-pp-sf.html", 800.09925, 41.9390225, 32.83905, 84.5881775),
        Frozen("cur_oat_apple_pear_porridge", "37178e14629de59ac02adf49a5fff49579872f87ccc34c0008a70d5bc60d6a95", "8b6bba491468f6b7b898015ad7bd367ee1271d2be7ba158c3a66ed17f86186a7", "https://www.jamieoliver.com/recipes/fruit/porridge-lots-of-ways/", 462.2113, 14.173, 10.0198, 80.9209),
        Frozen("cur_lentil_carrot_cream_soup", "13919913f35a2a9e4257342194a5076c2527269b89171cf695d10ea7c8681a1a", "e206aa9899a27df584cec11254a360af86050b06b21551e9bbbafd6ee4d1884f", "https://www.canada.ca/en/health-canada/services/food-guide/eating-support/kitchen/recipes/carrot-ginger-lentil-soup.html", 529.28, 27.188, 10.76, 85.484),
        Frozen("cur_buckwheat_mushroom_potato_soup", "675546d1f3a9b51b27df208904914be921f16ebe500c855b796e1250f825e1ac", "cfe48c9746e9aa572e0eebead0170d6ebff008e419c454a5b76ce7555b549d9b", "https://eng.obozrevatel.com/section-food/news-easy-buckwheat-soup-without-meat-a-perfect-dish-for-lunch-09-03-2024.html", 496.79, 17.101, 8.677, 96.201),
        Frozen("cur_potato_egg_cucumber_salad", "4369b651192d813daa58e3d65fa0daf0ee93aca0e3224dcb9a1d519e7627d523", "a8a7dd6c5de77480610e73a8719afe9ecaa7c40da2812258338a1804299c18e2", "https://www.zwilling.com/uk/z-cut-recipes/potato-cucumber-salad.html", 453.697375, 23.78225, 14.615875, 58.446625),
        Frozen("cur_zucchini_lentil_tomato_boats", "2abad320b8dc30f7c1185d81a76bdb563ca799b913f00609a056f50cee67c215", "f50b632f19ca093648c5034a026d36e989e8a0a1ce577cd9b75736b379180f7f", "https://theplantbasedschool.com/zucchini-boats/", 539.5, 36.016, 13.312, 75.0985),
        Frozen("cur_broccoli_cheese_pasta_sauce", "604ed9d1f699d6c118273320823e2decb909e1dfdefac1446dbfb42932cc2933", "50432fdcce8aa7d3c131c02fbf201135a46bf2469758fe0903adc11bcd9699e2", "https://rebeccawilson.com/recipe/cheesy-broccoli-10-minute-pasta/", 647.28, 33.6132, 19.5858, 86.55),
        Frozen("cur_baked_potato_cottage_cucumber", "e7a3d2111762a6d29edcb4b40f1ffb9249efedc005f82c2a90154300dc178a62", "f4eb4b400219962152d1989988cb3ec59a18c2feda7abb41f9b2ead5a5f065b5", "https://www.coop.co.uk/recipes/jacket-potatoes-with-herby-cottage-cheese", 469.539, 31.1495, 9.0575, 67.534),
        Frozen("b5_potato_egg_nests_oven", "8171c201a7441b4eb2a3d3642620cb6351b6164a39e57fc690d230f25e0fdc55", "5b88db069f2711fdc1506b5762fb656c1be7e8452d3ead2e3eb0c3e47148c63b", "https://www.lovefromtheland.com/baked-eggs-nests", 536.83, 27.586, 27.112, 47.8305),
        Frozen("b5_lazy_cottage_dumplings_pear", "b8326b8885cc850b4ef20351cd235ea54eba866dc2d91b3e145ac8e24e29252c", "879252e896920cb889cfbfeaf3a88048384f0fb6d9796f7ab2aeb5c0c1980d90", "https://www.polishyourkitchen.com/polish-lazy-pierogi-pierogi-leniwe", 656.79195, 46.69775, 18.33195, 76.3531),
        Frozen("b5_berry_banana_smoothie_bowl", "480e63f0b024ba95509b9d953c8994e381796923d79422b698d320ba4293b522", "5d41dcf72e98752b469ce88545ff7c53a629d29e4cb02e7f0bd4741acfd48365", "https://dairyfarmersofcanada.ca/en/canadian-goodness/recipes/breakfast-muesli-smoothie-bowl", 585.3, 20.003, 14.3445, 100.2005),
        Frozen("cur_baked_pear_cottage_berries", "19636d48efc7d2b5d9a76160ab8bdb87ed2e98f760c003e0da8a1d104007121e", "8a51e4883111f0b6438c89ca2d84bab968a6c1afe0045019c9c15fdbea0b751b", "https://ls-eng.obozrevatel.com/section-ls-food/news-tastier-than-any-dessert-healthy-baked-pears-with-cheese-and-honey-12-09-2024.html", 334.55647, 23.58175, 9.06447, 40.45556)
    )
    private val rejected = setOf(
        "cur_turkey_potato_roast",
        "cur_chickpea_veg_patties",
        "cur_turkey_millet_soup",
        "cur_millet_apple_fritters",
        "b5_warm_chickpea_mushroom_broccoli_salad",
        "cur_rice_pear_pudding",
        "cur_beef_cabbage_shchi"
    )

    @Test fun intakeHasExactly25IndependentOutcomes() {
        assertEquals(18, frozen.size)
        assertEquals(7, rejected.size)
        assertEquals(25, frozen.size + rejected.size)
        assertEquals(18, frozen.map { it.id }.toSet().size)
        assertTrue(frozen.none { it.id in rejected })
        assertEquals(18, M2PublishedMethodBulkFourth20261025.reviews.size)
        assertEquals(89, M2PublishedMethodReviews.reviews.size)
        for (id in rejected) {
            assertFalse("Rejected candidate must be absent from this batch: $id",
                M2PublishedMethodBulkFourth20261025.reviews.containsKey(id))
        }
    }

    @Test fun allEighteenHavePinnedPublishedMethodsAndIndependentRawMacros() {
        for (row in frozen) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == row.id }
            val review = M2PublishedMethodBulkFourth20261025.reviews.getValue(row.id)
            assertEquals(row.recipeSha, RecipeEvidenceAudit.fingerprint(recipe))
            assertEquals(row.recipeSha, review.recipeFingerprintSha256)
            assertEquals(row.publishedSource, review.publishedMethodSource)
            assertEquals(row.excerptSha, review.methodSnapshotSha256)
            assertEquals(row.excerptSha, RecipeEvidenceAudit.sha256Utf8(
                requireNotNull(review.methodSnapshotText)))
            assertEquals(RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE, review.nutritionBasis)
            assertEquals(row.id, review.recipeId)
            val recorded = requireNotNull(review.independentlyRecalculatedRawNutrition)
            val production = OfflineRecipes.nutrition(recipe)
            assertEquals("${row.id} independently recorded kcal", row.kcal, recorded.kcal, 0.00001)
            assertEquals("${row.id} independently recorded protein", row.protein, recorded.protein, 0.00001)
            assertEquals("${row.id} independently recorded fat", row.fat, recorded.fat, 0.00001)
            assertEquals("${row.id} independently recorded carbs", row.carbs, recorded.carbs, 0.00001)
            assertEquals("${row.id} production kcal", row.kcal, production.kcal, 0.00001)
            assertEquals("${row.id} production protein", row.protein, production.protein, 0.00001)
            assertEquals("${row.id} production fat", row.fat, production.fat, 0.00001)
            assertEquals("${row.id} production carbs", row.carbs, production.carbs, 0.00001)
            val audit = RecipeEvidenceAudit.inspect(recipe, review)
            assertTrue("${row.id}: ${audit.issues}", audit.documentedReady)
            assertFalse(audit.skuLabelCleared)
            assertFalse(audit.physicalTrialDocumented)
            assertTrue(RecipeAuditNotice.FINISHED_YIELD_UNKNOWN in audit.notices)
            assertTrue(RecipeAuditNotice.RAW_INPUT_NUTRITION_APPROXIMATE in audit.notices)
        }
    }

    @Test fun allEighteenFailClosedAfterEditedMethodRecipeOrArithmetic() {
        for (row in frozen) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == row.id }
            val evidence = M2PublishedMethodBulkFourth20261025.reviews.getValue(row.id)
            val alteredCapture = evidence.copy(
                methodSnapshotText = requireNotNull(evidence.methodSnapshotText) + " tampered")
            assertTrue(RecipeAuditIssue.METHOD_SNAPSHOT_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, alteredCapture).issues)
            assertFalse(RecipeEvidenceAudit.inspect(recipe, alteredCapture).documentedReady)

            val alteredRecipe = recipe.copy(ingredients = recipe.ingredients.mapIndexed { i, input ->
                if (i == 0) input.copy(grams = input.grams + 2) else input
            })
            assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE in
                RecipeEvidenceAudit.inspect(alteredRecipe, evidence).issues)

            val raw = requireNotNull(evidence.independentlyRecalculatedRawNutrition)
            val wrongCalculation = evidence.copy(independentlyRecalculatedRawNutrition =
                raw.copy(carbs = raw.carbs + 2.0))
            assertTrue(RecipeAuditIssue.NUTRITION_RECALCULATION_MISMATCHED in
                RecipeEvidenceAudit.inspect(recipe, wrongCalculation).issues)
            assertFalse(RecipeEvidenceAudit.inspect(recipe, wrongCalculation).documentedReady)
        }
    }

    @Test fun excludedRecipesAndGeneratedTemplatesCannotCompleteM2() {
        val report = RecipeEvidenceAudit.report()
        assertEquals(350, report.total)
        assertEquals(150, report.distinctDrafts)
        assertEquals(200, report.generatedTemplates)
        assertEquals(89, report.documentedReviewed)
        assertEquals(61, OfflineRecipes.curatedDrafts.count {
            it.id !in M2PublishedMethodReviews.reviews
        })
        assertEquals(0, report.skuLabelsClearedByRecipes)
        assertFalse(report.meetsM2Gate)
        for (id in rejected) {
            val current = OfflineRecipes.curatedDrafts.single { it.id == id }
            assertFalse(RecipeEvidenceAudit.inspect(current).documentedReady)
        }
    }
}
