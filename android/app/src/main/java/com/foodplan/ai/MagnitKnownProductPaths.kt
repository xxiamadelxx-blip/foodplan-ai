package com.foodplan.ai

import com.foodplan.domain.MagnitPackOnlySkuCatalog

/**
 * Previously inspected public Magnit product-page paths. No price is bundled.
 * Titles, quantities and fresh visible prices MUST still be checked at runtime.
 * This is a list of discovery entry points, not a local product database.
 */
internal object MagnitKnownProductPaths {
    data class MassSku(val url: String, val grams: Int)

    private val massSkus = mapOf(
        "rice" to MassSku("https://magnit.ru/product/3152910005-ris_dlinnozernyy_2_sort_800g_6",800),
        "buckwheat" to MassSku("https://magnit.ru/product/1342500014-grechka_yadritsa_mistral_900g",900),
        "oats" to MassSku("https://magnit.ru/product/3284010204-moya_tsena_krupa_gerkules_fas_400g_6",400),
        "millet" to MassSku("https://magnit.ru/product/3139480009-krupa_psheno_1s_fas_800g_6",800),
        "lentils" to MassSku("https://magnit.ru/product/1000567452-premiere_of_taste_chechevitsa_krasnaya_kolot_0_45kg_p_up",450),
        "flour" to MassSku("https://magnit.ru/product/1042229921-muka_pshenichnaya_khlebopekarnaya_2kg",2000),
        "pasta" to MassSku("https://magnit.ru/product/1319600015-makfa_makarony_rozhki_v_s_450g_20",450),
        "chicken" to MassSku("https://magnit.ru/product/1000233462-file_tsb_okhl_lotok_1_kg_v_lotok_ooo_soyuzptitseprom_5",1000),
        "carrot" to MassSku("https://magnit.ru/product/1445100176-morkov_mytaya_fasovannaya_1upak",1000),
        "cottage_cheese" to MassSku("https://magnit.ru/product/1000091079-korovka_iz_korenovki_tvorog_5_180g_fl_p_kmkk_12",180),
        "cheese" to MassSku("https://magnit.ru/product/8000032665-moloka_voz_syr_gauda_gold_45_180g_flp_belsyr",180),
        "mushroom" to MassSku("https://magnit.ru/product/9019013117-griby_shampinony_400g",400),
        "potato" to MassSku("https://magnit.ru/product/9072651210-kartofel_1kg",2000),
        "cucumber" to MassSku("https://magnit.ru/product/3412110001-ogurtsy_gladkie",500),
        "tomato" to MassSku("https://magnit.ru/product/1445100286-tomaty_600g",600),
        "broccoli" to MassSku("https://magnit.ru/product/1000348801-premiere_of_taste_kapusta_brokkoli_bystrozamorozhennaya_400g_polietilenovaya_upakovka_ooo_agama_roya",400),
        "cod" to MassSku("https://magnit.ru/product/1000239574-treska_file_600g",600),
        "tofu" to MassSku("https://magnit.ru/product/1000237883-syr_tofu_selskiy_dom_klassicheskiy_4_8_250g",250),
        "yogurt" to MassSku("https://magnit.ru/product/8000094915-solnyshko_kubani_yogurt_natur_2_5_160g_pl_st_molpromkuban",160),
        "salmon" to MassSku("https://magnit.ru/product/8000098000-fish_more_losos_steyk_s_m_na_kozhe_400g_v_u",400),
        "onion" to MassSku("https://magnit.ru/product/9072651204-luk_repchatyy",700),
        "garlic" to MassSku("https://magnit.ru/product/3412060011-chesnok",100),
        "cabbage" to MassSku("https://magnit.ru/product/9072651205-kapusta_belokochannaya_1kg",3000),
        "apple" to MassSku("https://magnit.ru/product/9039215501-yabloki_novyy_urozhay",800),
        "turkey" to MassSku("https://magnit.ru/product/1000261026-indilayt_file_grudki_indeyki_okhl500g_lotok_penzamolinvest_4",500),
        "lemon" to MassSku("https://magnit.ru/product/9072651504-limony",400),
        "pear" to MassSku("https://magnit.ru/product/1000521260-grushi_golden",700),
        "bell_pepper" to MassSku("https://magnit.ru/product/1444282021-perets_zhyeltyy_1kg",300),
        "zucchini" to MassSku("https://magnit.ru/product/3412080003-kabachki",700),
        "milk" to MassSku("https://magnit.ru/product/1000091750-moloko_vkusnoteevo_ultrapasterizovannoe_3_2_900g",900),
        "beef" to MassSku("https://magnit.ru/product/1000301762-steyk_miratorg_black_angus_tender_mramornaya_govyadina_260g",260)
    )

    val knownIngredientIds: Set<String> =
        massSkus.keys + MagnitPackOnlySkuCatalog.entries.map { it.ingredientId }

    fun urlFor(ingredientId: String): String? =
        massSkus[ingredientId]?.url
            ?: MagnitPackOnlySkuCatalog.entries.firstOrNull {
                it.ingredientId == ingredientId
            }?.url

    fun expectedGrams(ingredientId: String): Int? = massSkus[ingredientId]?.grams
}
