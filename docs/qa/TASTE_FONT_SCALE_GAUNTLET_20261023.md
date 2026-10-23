# FoodPlan · Large-system-font survey Gauntlet · 2026-10-23

## User goal and scope

Keep all four choices in the offline 25-dish taste survey accessible on a narrow phone at increased font size, without losing prior taste votes or the saved weekly menu. No runtime AI, remote fonts, downloadable images or additional screens.

## Real implementation

- [b364292](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/b364292090e97bd72af753caae27e73c15611907) implements a size- and font-scale-aware `TasteSurveyScreen`. The illustration/title/description area scrolls; four vote buttons and the Back/Open app actions remain in the stable bottom region. The 25 prepackaged food VectorDrawables and IDs are unchanged.
- [669665b](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/669665b9df72b5a7bf6c658cc515b576d2228f62) adds an Android Compose test at **320×550 dp / 180% font scale**, asserting the four answer targets, navigation, and successful 25-card completion.
- [f8166fb](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/f8166fb6b9649742394197fd5ccb1c727d65cfef) corrects a regression: on a small screen with **normal** font size the progress text retains its preexisting wording. This avoids altering the existing verified navigation contract.

## Three Gauntlet attempts (no fourth retry for the same external probe)

1. [Android CI #39965192200](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39965192200): JVM/debug APK **PASS**; UI suite **48/51 passed, 3 failed** due to normal-font progress label regression. Corrective code: f8166fb.
2. [Android CI #39966557572](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39966557572): **51/51 Android instrumentation tests PASS** including the 180%-font/320×550dp Compose test; separate OS-level font-scale command **FAIL**, because the emulator runner executes every YAML script line separately and did not preserve multi-line ADB arguments. Corrective CI command: [25fc87a](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/25fc87a9ec3375085a1af0c08fae451a1a8445d6).
3. [Android CI #39967791480](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39967791480): **51/51 Android instrumentation tests PASS**; extra Android ADB instrumentation **FAIL** with `Unable to find instrumentation info for com.foodplan.ai.test/AndroidJUnitRunner` after the preceding Gradle test task. Root cause: instrumentation target no longer installed at the time of standalone ADB command.

**Decision:** Larger-font UI *within an Android Compose test* is PASS (normal and 180% font fixture, 320×550dp compact constraints). A second standalone **actual OS font-setting** test is **BLOCKED / NOT VERIFIED** after three Gauntlet attempts. Do not recast this as a full physical accessibility, actual device TalkBack, or real system setting PASS. Also do not claim the UI is visually beautiful merely because semantic tests pass.

[9ed60b5](https://github.com/xxiamadelxx-blip/foodplan-ai/commit/9ed60b5608887af45aa43f52577b7393088c12ec) removes ONLY the unstable additional ADB probe, retaining the 180% Compose instrumentation and already accepted M5 ADB process-death test. [Restoration CI #39969064796](https://github.com/xxiamadelxx-blip/foodplan-ai/actions/runs/39969064796) pending when this report was first written.

## External source and release boundaries

Separate live M4 source probes #39965192109 and #39966557853 failed with HTTPError/UNKNOWN on public retailer pages. Old 33 observed public SKU prices are historical evidence, **not newly confirmed prices**. Do not fabricate source values, retail stock, exact gram-equivalents of eggs/oil, or a complete seven-day total.

M2 remains BLOCKED (0/150 independently certified cooked recipe methods/yields), M3 partial, M4 full week UNKNOWN, physical M5 unverified, M6 permanent signed public release blocked. This UI improvement is no new M7 milestone and must not be counted as the whole product release.

**Next separate work:** once restoration CI is inspected, record PASS/FAIL for the restored baseline. Do not run a fourth attempt of the standalone font probe without a newly approved scope. Move to independent release acceptance criteria and source evidence, failing closed on unavailable originals.
