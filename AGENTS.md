# FoodPlan AI agent instructions

## Product UX is a hard contract

Read [docs/USER_JOURNEY_CONTRACT.md](docs/USER_JOURNEY_CONTRACT.md) **before editing any Android UI, flow, recipe recommendation or retailer integration**.

The actual end-user application contains only **three primary sections: Продукты, Рецепты, Настройки**. The initial profile wizard and 25-dish taste survey run once, before these sections. Reopening the app opens normal usage, never forces onboarding again.

- **Продукты:** choose exact weekly budget (100..100000 ₽, integer ₽) and preferred Russian store, then see a seven-day shopping list, whole packs, actual known prices and explicit unknowns. Budget and store are usage choices, not required onboarding steps.
- **Рецепты:** choose a day, then a meal (завтрак/обед/ужин). View ingredients, preparation, grams, rough nutrition, choose effort **for this meal**, ask for another compatible dish, or cook this meal for N consecutive days. Household size from Settings applies to purchase quantities. Changes recalculate the whole week.
- **Настройки:** personal parameters, goal, dietary restrictions, allergies, food dislikes, dietary/religious requirements, kitchen equipment, household, cooking preferences and repeatable taste survey.

**Never add extra user tabs/screens exposing recipe graph, ingredient threads, parser passes, SKU matching, price provenance diagnostics, M2/M4 internals, test results, developer dashboards or automated ranking.** These are internal services. Human-readable product/price uncertainty is required but is not an engineering dashboard.

Active route: `MainActivity/FoodPlanApp → FoodPlanHomeScreen`. `WeeklyPlanScreen` is a legacy engineering preview **not part of the normal navigation**. Do not restore it as the main user interface or use it as a target for UX assumptions.

## Safety and honesty

- Allergies, ingredient exclusions and unverified religious food certification always override recommendation scores and savings. UNKNOWN labeling never proves safe.
- No invented live SKU prices, packages, discounts or stock. An incomplete basket cannot be called within budget.
- 100 ₽ must remain a valid input. Show truthful partial baskets if reliable prices exist and warn about nutrition deficits; otherwise explain no reliable prices exist.
- No checkout/cart manipulation, paid mandatory API or unwanted authentication.

## Engineering acceptance

Use Gauntlet Loop: implement → independent tests → inspect failures → correct → rerun on exact commit → write PASS/FAIL. Validate Kotlin JVM tests, debug APK and real cloud Android emulator when touching code. Record what remains unverified.

M2 requires ≥150 independently validated recipes and M4 requires actual lawful selected-store crawling and independently proven per-SKU labels. Synthetic tests never close these release gates.
