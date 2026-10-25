package com.foodplan.domain

import org.junit.Assert.*
import org.junit.Test

class M2PublishedMethodBulkSecond20261024Test {
    private data class Frozen(val id: String, val fingerprint: String, val excerpt: String,
        val kcal: Double, val p: Double, val f: Double, val c: Double)
    private val checked = listOf(
        Frozen("cur_cottage_apple", "31d4e2f2dbfeb93b1b010f50ade7d370f8fbfdda05e4c4702bcac71311ad33ab", "12fc14d13e9631cea3674b628216dbe09caf7c35cbdc50a5d654a17680825a5d", 315.1, 29.646, 10.688, 27.23),
        Frozen("cur_egg_potato_hash", "5ae248ba9e3a6b25b7ccd2a5c762802e431b167d05ce5dbdd71f960b2e479405", "f21bac8264aceb72a99b60efa55526d870c5b219e09a7fccc86200bf1989f631", 472.25, 20.42, 22.012, 51.216),
        Frozen("cur_cod_tomato_pan", "06bee288d1605144f9996e1a3d2d7d99f9952baa19e8b576434f1ecf324a088c", "2af2f32fac7a52daa6e132e19ddeb61c259810cf918dab179e6b5640e6598238", 344.14955, 38.848375, 14.55875, 20.771),
        Frozen("cur_chicken_tomato_rice", "70ec7402c370e085a69290294c1d68270bd1a6a971edeb99ca9b6fe0b5174b50", "f04bea17e79b98947650f009d540ef892c33d8441d5fe5ad9e5d4d74b39211f0", 617.3852, 48.59608, 13.7676, 77.63452),
        Frozen("cur_turkey_zucchini_pasta", "a552b18bc773e3b39dea9c3a2a89dd8a46754d57cd21dd5c563a3a1330035336", "9ad15c2a36b3544e2b784d474d02fe1f7b0c305b94fadde36bd98572c6638ff2", 640.13, 54.1232, 14.4623, 71.6195),
        Frozen("cur_lentil_mushroom_soup", "017824acf7424a06ab02691aa4dcb924026a7035643160b090e7983931d6fa51", "6b8a6acb38844a95981b9e73da480afdcf6cfafce99ed8fa5508e15164454cc2", 576.88, 30.8155, 8.895, 100.3175),
        Frozen("cur_tofu_pepper_buckwheat", "0380b01a0d47e1861aa671c12f715b2bfdf02cb8d22e33ee557bb5b56c333d55", "25df23568fe95c4e0f6a70c6b78f402fd4633bc2c6cc75450e677e89aadaace1", 643.16, 32.7954, 22.2181, 90.3455),
        Frozen("cur_pork_mushroom_pasta", "212fb204c390920bbcd40e4c8d21c3c1e3756438e8e971445546546cf06b00b1", "986a0ff8051b84b485e0537c5811c6da1d9113d548f5bccd6b3bfb210b7bebed", 859.51, 55.536, 35.0795, 80.6695),
        Frozen("cur_chicken_lentil_bowl", "47139160f65479087bdc6cd5365f78eb2584d7e5ca44ecb8f21affe6e98d7179", "426445d06f960cc6b6639ceec3ccc1db9bb28e7e67e92e19713e242bc87a9e71", 695.7212, 67.4525, 14.3082, 80.861),
        Frozen("cur_cottage_pear_millet_bowl", "2fd821daeb4c2ff83c6e0884b163fa0e4e7b3053429cbb92f4df5d5da6355bef", "08c3fc2814e054fbdd41388948af17380f188f0f5538942e9adc7641f45c53a7", 596.42582, 33.3615, 14.55182, 82.26136),
        Frozen("cur_savory_oats_egg_mushroom", "5defda1cc60facff9d6952ec5bbd56c88ca6d55871416f036de1179a6d1e9306", "a56aee25ee23176339692c149a7b35c82d810434373b5ef5720b3558f7261531", 529.08, 27.302, 23.002, 56.42),
        Frozen("cur_baked_cottage_berries", "e7a658cae21cb3e94df9a0425ccad73b2deb9475e8b16b655cf3e0cda668a8a8", "19617ee5a18a2bdcf61e3d5e939a315e66818dfb6c05113ed29bf0c509578a69", 480.06, 41.313, 20.022, 34.969),
        Frozen("cur_egg_mushroom_roll", "29fc4d74b7f6996f66805446f1d2d003fc21084706bf37cdcfd17ba3386911c9", "b2077661d9ac36034af23907bd5afa6ecdabdabc578dded55e3218e6160e1321", 299.43, 21.0805, 21.7845, 6.931),
        Frozen("cur_millet_pumpkinless_pear", "243153bef0e6b8ddb889fec9d421ef2394a988de66e6832fd8df69a626ca5e67", "31e8facc6647cd13ebf40586665be19db823b7f63b3f49c67549141a54a5ed40", 487.59195, 15.51175, 11.12795, 80.8556),
    )
    private val rejected = setOf(
        "cur_turkey_buckwheat",
        "cur_baked_apple_oats",
        "cur_pork_buckwheat",
        "cur_beef_zucchini_rice",
        "cur_cod_fish_soup",
        "cur_mushroom_cheese_buckwheat_bake",
    )
    @Test fun oneTwentyDishIntakeAcceptsFourteenAndRejectsSix() {
        assertEquals(20, checked.size + rejected.size)
        assertEquals(14, checked.size)
        assertEquals(14, M2PublishedMethodBulkSecond20261024.reviews.size)
        assertEquals(75, M2PublishedMethodReviews.reviews.size)
        assertTrue(checked.map { it.id }.toSet().intersect(rejected).isEmpty())
        assertTrue(M2PublishedMethodBulkSecond20261024.reviews.keys.intersect(rejected).isEmpty())
        assertEquals(75, OfflineRecipes.curatedDrafts.count { it.id !in M2PublishedMethodReviews.reviews })
    }
    @Test fun allFourteenPublishedMethodsPinAuthoredRecipeSourceAndRawArithmetic() {
        for (frozen in checked) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == frozen.id }
            val evidence = M2PublishedMethodBulkSecond20261024.reviews.getValue(frozen.id)
            assertEquals(frozen.fingerprint, RecipeEvidenceAudit.fingerprint(recipe))
            assertEquals(frozen.fingerprint, evidence.recipeFingerprintSha256)
            assertEquals(frozen.excerpt, evidence.methodSnapshotSha256)
            assertEquals(frozen.excerpt, RecipeEvidenceAudit.sha256Utf8(requireNotNull(evidence.methodSnapshotText)))
            assertEquals(RecipeNutritionBasis.RAW_EDIBLE_INPUT_ESTIMATE, evidence.nutritionBasis)
            val raw = requireNotNull(evidence.independentlyRecalculatedRawNutrition)
            val actual = OfflineRecipes.nutrition(recipe)
            assertEquals(frozen.kcal, raw.kcal, 0.0000001)
            assertEquals(frozen.p, raw.protein, 0.0000001)
            assertEquals(frozen.f, raw.fat, 0.0000001)
            assertEquals(frozen.c, raw.carbs, 0.0000001)
            assertEquals(frozen.kcal, actual.kcal, 0.00001)
            assertEquals(frozen.p, actual.protein, 0.00001)
            assertEquals(frozen.f, actual.fat, 0.00001)
            assertEquals(frozen.c, actual.carbs, 0.00001)
            val result = RecipeEvidenceAudit.inspect(recipe, evidence)
            assertTrue("${frozen.id}: ${result.issues}", result.documentedReady)
            assertFalse(result.skuLabelCleared)
            assertFalse(result.physicalTrialDocumented)
            assertTrue(RecipeAuditNotice.FINISHED_YIELD_UNKNOWN in result.notices)
        }
    }
    @Test fun everySourceRejectsChangedRecipeAlteredExcerptAndFalseNutrition() {
        for (frozen in checked) {
            val recipe = OfflineRecipes.curatedDrafts.single { it.id == frozen.id }
            val ev = M2PublishedMethodBulkSecond20261024.reviews.getValue(frozen.id)
            assertTrue(RecipeAuditIssue.METHOD_SNAPSHOT_MISMATCHED in RecipeEvidenceAudit.inspect(recipe,
                ev.copy(methodSnapshotText = "Tampered evidence summary, should not match")).issues)
            val stale = recipe.copy(ingredients = recipe.ingredients.mapIndexed { i, part ->
                if (i == 0) part.copy(grams = part.grams + 2) else part
            })
            assertTrue(RecipeAuditIssue.DOCUMENTARY_REVIEW_STALE in RecipeEvidenceAudit.inspect(stale,ev).issues)
            val raw = requireNotNull(ev.independentlyRecalculatedRawNutrition)
            assertTrue(RecipeAuditIssue.NUTRITION_RECALCULATION_MISMATCHED in RecipeEvidenceAudit.inspect(recipe,
                ev.copy(independentlyRecalculatedRawNutrition = raw.copy(fat = raw.fat + 5.0))).issues)
        }
    }
}
