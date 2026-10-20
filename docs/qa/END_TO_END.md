# FoodPlan AI: end-to-end prototype test contract

**Status:** in development. Full product M0–M6 NOT ACCEPTED.

A test profile must be able to:
1. Save local user profile and complete 25 taste ratings.
2. Generate 7 days with 1–6 meals/day (subject to recipe availability).
3. See preliminary kcal/protein/fat/carbohydrate and per-recipe raw ingredient masses.
4. Open step-by-step instructions for each selected meal.
5. View a single aggregated week shopping list in grams and estimated packaging counts.
6. Be told prominently that data are unverified and there are NO live Russian store prices; do not claim budget compliance.
7. Reject unknown free-text allergens, missing kitchen equipment and unsafe/unsupported user profiles; no silent default substitutes.
8. Re-run deterministic algorithm on changed profile or votes and avoid immediate recipe repeats where possible.

**Known gaps blocking release:** 
- 200 algorithmic recipe combinations are *draft prototypes*, not 150 verified standalone recipes.
- Food composition numbers are provisional estimates, not sourced to product/official records.
- Ingredient synonyms, allergens from traces and facility cross-contact are not verified.
- No live Russian store inventory/price, no user receipts/manual cost entry yet, no guaranteed budget.
- No persistence of generated plan, pantry editing, replacement meal recalculation or 100% completeness guarantee.
- Energy target formula uses broad non-clinical approximations; not an individual health prescription.

**Acceptance:** domain JVM tests + Android emulator instrumentation + tangible shopping/recipe screens, plus independent manual audit later. A passing emulator smoke alone does not close M2–M6.
