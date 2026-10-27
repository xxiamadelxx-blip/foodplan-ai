# FoodPlan YAR-6 · next bulk candidate intake (18 unresolved IDs)

**State: TRIAGE ONLY.** This file is not documentary review evidence. Last confirmed M2 total **39/150** on [CI #40128622385](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/40128622385). No method URLs, SHA digests or independent calculations have been invented. The selection below was obtained by comparing the first 44 author-draft declarations against the current Kotlin source-reviewed registry; it is a partial prioritized sample, **not** a full catalog-wide reconciliation. These are all unaccepted IDs.

| ID | Priority / reason | State |
|---|---|---|
| `cur_yogurt_berries_oat` | 15-minute oat softening not supported by overnight source | `METHOD_DURATION_MISMATCH` |
| `cur_chickpea_salad` | whole peeled lemon FDC is not lemon juice used in steps | `INGREDIENT_BASIS_MISMATCH` |
| `cur_greek_salad` | needs independent salad method and cheese type verification | `SOURCE_NEEDED` |
| `cur_cod_potato_tray` | needs source for staged 200C potato then cod/zucchini and lemon basis | `SOURCE_NEEDED` |
| `cur_salmon_broccoli_tray` | needs source for staged potatoes then salmon/broccoli and lemon basis | `SOURCE_NEEDED` |
| `cur_turkey_buckwheat` | needs method for turkey pan-braise with separately cooked buckwheat | `SOURCE_NEEDED` |
| `cur_baked_potato_chickpeas` | needs published staged roasted potato plus canned chickpeas | `SOURCE_NEEDED` |
| `cur_baked_apple_oats` | needs oven-baked whole apple with water-moistened oats method | `SOURCE_NEEDED` |
| `cur_cottage_banana_bowl` | needs independent no-cook cottage cheese banana dry oats source | `SOURCE_NEEDED` |
| `cur_turkey_stuffed_peppers` | needs precise stuffing/oven time and temperature method source | `SOURCE_NEEDED` |
| `cur_salmon_potato_salad` | needs method and fish cooling/safe-cooking comparison | `SOURCE_NEEDED` |
| `cur_lemon_chicken_rice` | needs exact chicken/rice/lemon preparation and lemon basis | `SOURCE_NEEDED` |
| `cur_turkey_lentil_stew` | needs dry-lentil and turkey simmer time method source | `SOURCE_NEEDED` |
| `cur_beef_cabbage_rolls` | needs independent filling, rolling, simmering method | `SOURCE_NEEDED` |
| `cur_chicken_potato_gratin` | needs safe chicken + sliced potato oven method | `SOURCE_NEEDED` |
| `cur_salmon_fishcakes` | needs cooked salmon and potato binding/pan-frying method | `SOURCE_NEEDED` |
| `cur_cabbage_mushroom_dumplings` | needs safe filling and dough/wrapper method source | `SOURCE_NEEDED` |
| `cur_pork_buckwheat` | needs pork/mushroom pan preparation and dry buckwheat method | `SOURCE_NEEDED` |

## Next independent acceptance work

1. Retrieve published original recipe instructions, confirm method applicability and document substantive substitutions for each candidate; reject materially different methods.
2. Pin exact recipe fingerprint and a faithful own-language source-method excerpt SHA, documenting capture date and page URL; digest only authenticates local excerpt, not the remote page.
3. Independently recompute raw edible kcal/P/F/C from current pinned ingredient bases; explicitly distinguish dry/cooked, lemon flesh/juice, canned drained/undrained and oil input/retention.
4. Admit only reviewed IDs with immutable evidence into Kotlin registry. Tests must fail for source-text mutation, stale recipe, wrong arithmetic and duplicate ID.
5. Run one JVM+APK+Android emulator Gauntlet per code batch. Record PASS count and exact executable SHA, keep unaccepted recipes PENDING. Do not certify cooked nutrition or SKU label allergens.

This queue intentionally does not change 39/150. The requested bulk size of 20–25 is a **target intake size**, not a license to fabricate source matches or pass results.
