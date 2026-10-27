# M4 next acceptance: full-week public price coverage

**Status: NOT ACCEPTED.** A two-item pilot is not a weekly shopping list.

Implement a deterministic end-to-end test on a real generated 7-day plan:
1. Generate a fixed, documented profile and 7-day menu with the production planner; export its *actual* aggregated shopping list, ingredient IDs and grams. Never hand-pick a convenient subset.
2. For every distinct ingredient, collect current public retailer SKU evidence (title, package mass, visible price, official URL, timestamp). No user address, account, local availability, AI or paid API. Respect robots and rate limits.
3. Apply production RetailWeekQuoter to the exact generated list. Preserve missingIngredientIds and null completeEstimateKopeks for every missing price; no zero-priced substitution. Calculate whole packages and exact kopeks.
4. Run budgets 100, 356, 63218, 100000 rubles. Show whether a **complete** estimated basket is affordable, and explicitly flag a known subtotal that already exceeds budget even if incomplete.
5. Save independently inspectable evidence: source URLs, observation time, all requested ingredients, every chosen SKU and cost, unknown entries, final totals and Android test logs.
6. Repeat on Android emulator and compare the result with a separate reference implementation. PASS only if every ingredient has a real price and both implementations agree. If a site blocks access, record BLOCKED and move on to the next provider rather than fabricate prices.

**Important regression:** Ensure the scanner's async timeout cannot hang after a missing listing; ensure no product name cross-matches a different ingredient. Strict allergies remain a separate eligibility gate, never inferred from a public price.

Previous independently verified baseline:
- Rice 800g: 49.99 RUB, buckwheat 900g: 129.99 RUB.
- 2-item whole-package quote: 229.97 RUB.
- 43 Android instrumented tests PASS, GitHub Actions #460.
- These prove a pilot only, not a full-week checkout estimate.

**Stop rule:** Once all acceptance tests pass, mark M4 accepted and advance to M5. If blocked by a missing external SKU, log exact missing ingredient and source evidence, then proceed to the next distinct ingredient or retailer; do not endlessly rework one source.
