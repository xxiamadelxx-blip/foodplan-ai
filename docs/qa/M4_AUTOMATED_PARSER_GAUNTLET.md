# M4 No-AI store parser: Gauntlet status

As of 2026-10-21. Scope: incremental PR #1, **not accepted** as a functional retailer crawler.

| Gate | Status | Evidence |
| --- | --- | --- |
| Requirements frozen in repository | PASS | [M4 spec](../M4_AUTOMATED_RETAIL_PARSER.md) |
| 37 generic search queries (brand-free) | LOCAL PASS | NeutralRetailQueries; local kotlinc compile and smoke (37 map keys, chicken before rice) |
| Repeat shopping items aggregate grams | LOCAL PASS | RetailScanPlanner smoke |
| Deterministic SKU compare, 120 vs 400 RUB, variety/ingredients | LOCAL PASS | DeterministicSkuComparator smoke |
| JVM unit tests via CI | PENDING | New JUnit tests in repository, awaiting exact-head Android workflow |
| Full Android APK + emulator on this head | PENDING | [Current Android CI workflow](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/workflows/) |
| Live retailer automatic category crawl | FAIL | Android prototype reads visible Magnit DOM only after manual page navigation |
| Live SKU card ingredients / producer | FAIL | No independently confirmed live extraction from per-SKU pages |
| Address-confirmed stock/checkout price | FAIL | Unavailable |
| Independent labeled audit >=100 SKU | FAIL | Not performed |
| M4 overall | **FAIL** | End-to-end requirements unmet |
| M2 overall | **FAIL** | 0/150 independently accepted recipes; this code does not replace M2 evidence |

The local compiler and smoke checks were executed before the connector-created files were pushed; they are not a substitute for the versioned GitHub CI checks. Do not promote PASS until the exact-head JUnit+Android+emulator jobs have succeeded.

Sequence to continue: integrate query factory with selected-store adapter; pilot chicken; scan all neutral needs; extract SKU source provenance, label/producer; verify safety and equivalence; optimize whole basket; independently audit live results.
