package com.foodplan.ai

/**
 * Public official catalogue entrypoints already checked by independent M4
 * source investigations. These are only PATHS: prices are NEVER bundled.
 * On-device parser must observe a CURRENT visible title, package and price
 * before saving a quote. Unsupported items fall back to public categories.
 */
internal object MagnitKnownSkuCatalog {
    val urls: Map<String, String> = linkedMapOf(
        "rice" to "https://magnit.ru/product/3152910005-ris_dlinnozernyy_2_sort_800g_6",
        "oats" to "https://magnit.ru/product/3284010204-moya_tsena_krupa_gerkules_fas_400g_6",
        "buckwheat" to "https://magnit.ru/product/1342500014-grechka_yadritsa_mistral_900g",
        "millet" to "https://magnit.ru/product/3139480009-krupa_psheno_1s_fas_800g_6",
        "lentils" to "https://magnit.ru/product/1000567452-premiere_of_taste_chechevitsa_krasnaya_kolot_0_45kg_p_up",
        "flour" to "https://magnit.ru/product/1042229921-muka_pshenichnaya_khlebopekarnaya_2kg",
        "pasta" to "https://magnit.ru/product/1319600015-makfa_makarony_rozhki_v_s_450g_20",
        "chicken" to "https://magnit.ru/product/1000233462-file_tsb_okhl_lotok_1_kg_v_lotok_ooo_soyuzptitseprom_5",
        "carrot" to "https://magnit.ru/product/1445100176-morkov_mytaya_fasovannaya_1upak",
        "cottage_cheese" to "https://magnit.ru/product/1000091079-korovka_iz_korenovki_tvorog_5_180g_fl_p_kmkk_12",
        "cheese" to "https://magnit.ru/product/8000032665-moloka_voz_syr_gauda_gold_45_180g_flp_belsyr",
        "mushroom" to "https://magnit.ru/product/9019013117-griby_shampinony_400g",
        "potato" to "https://magnit.ru/product/9072651210-kartofel_1kg",
        "cucumber" to "https://magnit.ru/product/3412110001-ogurtsy_gladkie",
        "tomato" to "https://magnit.ru/product/1445100286-tomaty_600g",
        "broccoli" to "https://magnit.ru/product/1000348801-premiere_of_taste_kapusta_brokkoli_bystrozamorozhennaya_400g_polietilenovaya_upakovka_ooo_agama_roya",
        "cod" to "https://magnit.ru/product/1000239574-treska_file_600g",
        "tofu" to "https://magnit.ru/product/1000237883-syr_tofu_selskiy_dom_klassicheskiy_4_8_250g",
        "yogurt" to "https://magnit.ru/product/8000094915-solnyshko_kubani_yogurt_natur_2_5_160g_pl_st_molpromkuban",
        "salmon" to "https://magnit.ru/product/8000098000-fish_more_losos_steyk_s_m_na_kozhe_400g_v_u",
        "onion" to "https://magnit.ru/product/9072651204-luk_repchatyy",
        "garlic" to "https://magnit.ru/product/3412060011-chesnok",
        "cabbage" to "https://magnit.ru/product/9072651205-kapusta_belokochannaya_1kg",
        "apple" to "https://magnit.ru/product/9039215501-yabloki_novyy_urozhay",
        "turkey" to "https://magnit.ru/product/1000261026-indilayt_file_grudki_indeyki_okhl500g_lotok_penzamolinvest_4",
        "lemon" to "https://magnit.ru/product/9072651504-limony",
        "pear" to "https://magnit.ru/product/1000521260-grushi_golden",
        "bell_pepper" to "https://magnit.ru/product/1444282021-perets_zhyeltyy_1kg",
        "zucchini" to "https://magnit.ru/product/3412080003-kabachki",
        "milk" to "https://magnit.ru/product/1000091750-moloko_vkusnoteevo_ultrapasterizovannoe_3_2_900g",
        "beef" to "https://magnit.ru/product/1000301762-steyk_miratorg_black_angus_tender_mramornaya_govyadina_260g",
        "banana" to "https://magnit.ru/product/9072651501-banany",
        "chickpeas" to "https://magnit.ru/product/8000013399-green_ribbon_nut_400g_zh_b_s_kl",
        "egg" to "https://magnit.ru/product/3454700001-yaytso_stolovoe_s1_10sht_boks_20",
        "oil" to "https://magnit.ru/product/1000029331-maslo_podsolnechnoe_rafinirovannoe_dezodorirovannoe_900ml"
    )

    fun urlFor(ingredientId: String): String? = urls[ingredientId]

    init {
        require(urls.size == 35)
        require(urls.values.toSet().size == urls.size)
        require(urls.values.all {
            Regex("""^https://magnit[.]ru/product/[A-Za-z0-9_-]+$""").matches(it)
        })
    }
}
