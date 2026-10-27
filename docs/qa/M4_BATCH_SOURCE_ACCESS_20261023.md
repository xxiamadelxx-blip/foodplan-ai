# M4 · Public store batch-source Gauntlet · 2026-10-23

## Implemented and independently verified

- [a837442](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/a837442dfcb692f9c5d08bb8089e98792b52383e): `tools/m4_chain_price_batch.py` fetches official `robots.txt` once per batch, preserves independent per-SKU validation, writes explicit `NOT_VERIFIED` JSON when blocked, and stops further SKU requests on 401/403/429. It does not bypass access gates, invent price or use stale cached prices.
- [d6eeb2b](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/d6eeb2b944a5ff0aebe7958403ca4659ce536216): six negative/positive tests for single robots GET, blocked robots, rate limits, isolated 404, permission denial, invalid batches.
- [c1b95e7](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/c1b95e777be3106aa986b387f18610898ed55c7b): M4 CI uses batch source collection, keeps the downstream actual-week evidence even if the two-product pilot cannot be priced, and retains a strict independent source acceptance gate.

## Exact accepted CI

[M4 run #39984495735](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39984495735) **PASS**:
- Offline tests: parser 13 PASS, pilot 5 PASS, new batch 6 PASS, full-week coverage 9 PASS.
- Actual official-site observation: **33/33 publicly visible SKU package prices** with **34 GET calls** (one robots GET and 33 SKU GETs), source access `COMPLETED_WITH_SOURCE_OBSERVATIONS`.
- Actual production 7-day/21-meal shopping aggregation: **31/33 purchasable quantities known**, `PARTIAL_FULL_WEEK_PRICE_UNKNOWN`. The 2 unresolved conversions are `egg` 10 pieces vs edible recipe grams, `oil` 900 mL vs recipe grams.
- Known subtotal of 31 computed lines **7,833.73 ₽** (`783373` kopeks), **not** a complete weekly quote. `full_week_total_kopeks = null`; no checkout price, stock or physical SKU allergen clearance claimed.
- Rice publicly observed 49.99 ₽ and buckwheat 129.99 ₽; these are indicative chain prices only.
- [Android CI #39984495502](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39984495502) **PASS**: 51/51 instrumentation tests, M5 force-stop/cold relaunch 8/8. [M6 unsigned #39984495534](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39984495534) **PASS**, not distributable signed release.

## Unchanged blockers and next milestone

M2 independent cooking-method, cooked-yield and exact retail allergen review **0/150** (BLOCKED); M4 31/33 pack quantities (PARTIAL), M5 physical device unverified, M6 signing/distribution not accepted. Do not infer 10 eggs = 600 edible grams or 900 mL oil = 900 g. A valid independent evidence chain for each specific ingredient basis is required before a full-week price PASS.

The batch refactor is **PASS**; M4 release milestone remains **PARTIAL**, not PASS.
