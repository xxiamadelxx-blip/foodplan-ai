# FoodPlan AI agent instructions

## Product UX is a hard contract

**Before any work, read [CONTEXT.md](CONTEXT.md) and [MILESTONES.md](MILESTONES.md)**. For Android UI, tastes, groceries or recipes, also read [docs/USER_JOURNEY_CONTRACT.md](docs/USER_JOURNEY_CONTRACT.md). For non-release product suggestions read [docs/POST_RELEASE_BACKLOG.md](docs/POST_RELEASE_BACKLOG.md).

**User requirement capture is a mandatory write operation:** whenever the user adds, corrects, or clarifies a product requirement, update `CONTEXT.md` **in the same work cycle** and update the owning spec/backlog and `MILESTONES.md` (status + next concrete action). Do not only restate it in chat. Do not claim implementation PASS merely because a requirement has been documented.

**Clarification 2026-10-22:** Illustrated images belong specifically to the 25-card **taste-preference survey**, not an automatic mandate to decorate the diet/allergy selector. Default taste-survey examples should be familiar home foods instead of primarily tom yum, ramen and shrimp pasta; `NEVER_TRIED` stays neutral. Shopping lists should emphasize purchasable whole packages/pieces over internal grams, never display `egg edible portion` technical labels. The user confirmed ordinary app reopening preserves their list; OS force-stop on a physical device remains separately unverified. See `CONTEXT.md` and `docs/POST_RELEASE_BACKLOG.md` for detailed criteria.

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
- **Evidence workflows are not AI services:** M2 independently matches documented culinary methods to authored recipes, calculates approximate nutrition from appropriately sourced food composition and applicable yield/retention factors. Physical cooking/yield measurement is an optional, risk-based QA tier, never a gate for all 150. Programmatic checks may identify gaps but cannot fabricate or self-approve evidence. M4 crawlers are ordinary, permission-respecting software with robot directives, rate limits and fail-closed unknown fields.
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

M2 requires ≥150 reproducible sourced home recipes and defensible calculated approximate nutrition (including source-appropriate edible basis and published cooking yield/retention factors where applicable). **Do not demand 150 physical cook-and-weigh trials**; risk-based practical sampling is optional QA. Ingredient exclusions remain strict; exact SKU trace/allergen labels are verified separately, never inferred from recipe sources. See corrected M2 protocol 2026-10-24. Synthetic tests alone do not validate source-backed recipes. M4 estimates approximate **chain-level prices**, without user address or local stock; never conflate a public price with confirmed dietary eligibility.

## Binding development protocol (2026-10-22)

1. **Read `CONTEXT.md`, `MILESTONES.md` and `docs/ROADMAP.md` at the start of every work cycle.** Identify the first unfinished M0→M6 stage and its exact acceptance criteria. For new UX work inspect `docs/POST_RELEASE_BACKLOG.md` without expanding current milestones.
2. Implement ONLY the next missing criterion, not nice-to-have enhancements, speculative refactoring or a newly invented substage.
3. Run Gauntlet: implement → test → independent evidence → targeted fix → repeat. Log PASS/FAIL and the tested SHA.
4. Three failed iterations maximum for one blocker. Record **FAIL/BLOCKED** with evidence, keep milestone OPEN, then move to a distinct independent task. Never claim blocked work complete.
5. Do not retest an unchanged accepted feature unless a specific regression or new dependent code justifies it.
6. **After EVERY cycle update `MILESTONES.md`** with statuses, evidence, remaining blocker and the exact next action. Passing a gate means immediately start the next M stage when possible.
7. "Продолжай" means execute the next unfinished task and its necessary tests without stopping at a progress report or requiring new approval.
8. The fixed milestone order is M0→M1→M2→M3→M4→M5→M6. A newer experimental feature does not supersede a blocked earlier acceptance criterion. When M2 external food-review evidence is unavailable, log BLOCKED and perform an independently useful M3/M4 implementation task without marking the earlier stage PASS.
9. **Scope-specific correction:** No address, precise store location, checkout verification or local stock verification is required to compute indicative Russian chain prices. Manufacturer and allergen label safety remain separate from price estimates.



## Главная задача, подтверждена пользователем 23.10.2026

**Не застревать на двух продуктах или внешнем API.** Приоритет: работающий сквозной сценарий «профиль/тип питания → бюджет/магазин → список продуктов на 7 дней и рецепты → замена/усилие/партии → пересчитанный и сохранённый список». Никакие игры с SKU единицами не заменяют проверку полного пользовательского цикла. Для недоступной магазинной цены выводить UNKNOWN, но продолжать строить неделю. Не подменять это обещанием точного бюджета или полноценности питания. Исследования единичных товаров при достижении BLOCKED прекращать и переходить к независимым критериям. Нельзя делать отдельную микрозадачу новой итерацией M4 только потому, что пользователь сказал «Продолжай».
