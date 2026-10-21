# FoodPlan AI agent instructions

## Product UX is a hard contract

Read [docs/USER_JOURNEY_CONTRACT.md](docs/USER_JOURNEY_CONTRACT.md) **before editing any Android UI, flow, recipe recommendation or retailer integration**.

The actual end-user application contains only **three primary sections: Продукты, Рецепты, Настройки**. The initial profile wizard and 25-dish taste survey run once, before these sections. Reopening the app opens normal usage, never forces onboarding again.

- **Продукты:** choose exact weekly budget (100..100000 ₽, integer ₽) and preferred Russian store, then see a seven-day shopping list, whole packs, actual known prices and explicit unknowns. Budget and store are usage choices, not required onboarding steps.
- **Рецепты:** choose a day, then a meal (завтрак/обед/ужин). View ingredients, preparation, grams, rough nutrition, choose effort **for this meal**, ask for another compatible dish, or cook this meal for N consecutive days. Household size from Settings applies to purchase quantities. Changes recalculate the whole week.
- **Настройки:** personal parameters, goal, dietary restrictions, allergies, food dislikes, dietary/religious requirements, kitchen equipment, household, cooking preferences and repeatable taste survey.

**Never add extra user tabs/screens exposing recipe graph, ingredient threads, parser passes, SKU matching, price provenance diagnostics, M2/M4 internals, test results, developer dashboards or automated ranking.** These are internal services. Human-readable product/price uncertainty is required but is not an engineering dashboard.

Active route: `MainActivity/FoodPlanApp → FoodPlanHomeScreen`. `WeeklyPlanScreen` is a legacy engineering preview **not part of the normal navigation**. Do not restore it as the main user interface or use it as a target for UX assumptions.

## Absolute runtime architecture: no AI

**FoodPlan AI is a fully deterministic, automated Android meal planner, NOT an AI chatbot.** The app's name does not permit generative AI in its actual user workflow.

- **Zero generative inference for regular usage.** Initial onboarding, 25 taste votes, dietary and allergy filters, recipe ranking, the product→recipe graph, weekly menu, replacement, cooking effort, batch days, budgeting, substitutions, ingredient aggregation, whole-pack optimization, persistence and shopping lists run from explicit Kotlin algorithms and locally held evidence. They MUST work offline with no AI keys, cloud LLM calls, tokens or AI subscriptions.
- **Only external data retrieval is allowed:** permitted retailer/public-source adapters may fetch observed product pages and prices with ordinary parsers. If a store is blocked or offline, show unknown prices/partial evidence. Never silently switch to AI-generated SKU, labels or prices.
- **Evidence workflows are not AI services:** M2 independently verifies documented recipe sources and real cooking yields; programmatic checks may identify gaps but cannot fabricate or self-approve evidence. M4 crawlers are ordinary, permission-respecting software with robot directives, rate limits and fail-closed unknown fields.
- **Fail closed** for dietary/religious restrictions, unknown allergens and incomplete proof, including the extreme-budget fallback. Do not downgrade unknown diets to omnivorous by default.
- **Never suggest integrating Groq, OpenAI, Gemini, DeepSeek, Qwen or any other LLM as a way to make the runtime planner, browser parser or nutritional safety gates work.** Automated does not mean generative.
- Technical evidence and search traces remain invisible in the three-section app. Retain separate independent PASS/FAIL evidence for algorithm, source availability, source labeling and Android execution.

## Safety and honesty

- Allergies, ingredient exclusions and unverified religious food certification always override recommendation scores and savings. UNKNOWN labeling never proves safe.
- No invented live SKU prices, packages, discounts or stock. An incomplete basket cannot be called within budget.
- 100 ₽ must remain a valid input. Show truthful partial baskets if reliable prices exist and warn about nutrition deficits; otherwise explain no reliable prices exist.
- No checkout/cart manipulation, paid mandatory API or unwanted authentication.

## Engineering acceptance

Use Gauntlet Loop: implement → independent tests → inspect failures → correct → rerun on exact commit → write PASS/FAIL. Validate Kotlin JVM tests, debug APK and real cloud Android emulator when touching code. Record what remains unverified.

M2 requires ≥150 independently validated recipes and M4 requires actual lawful selected-store crawling and independently proven per-SKU labels. Synthetic tests never close these release gates.
