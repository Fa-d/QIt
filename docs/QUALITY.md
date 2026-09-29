# Quality guardrails

One command proves the codebase is formatted, statically clean, lint-clean, tested, visually
unchanged, covered and architecturally sound:

```
./gradlew qualityGate
```

Run it locally before pushing; CI (`.github/workflows/ci.yml`) runs the same command plus
`assembleDebug` on every push and pull request.

## What each gate checks

| Gate | Task(s) | What it proves | How to fix failures |
| --- | --- | --- | --- |
| Formatting | `spotlessCheck` (every subproject) | Kotlin + `.gradle.kts` follow ktlint (`intellij_idea` style, 120 cols) plus Compose rules (`io.nlopez.compose.rules`). Config lives in `.editorconfig`. | `./gradlew spotlessApply` |
| Static analysis | `detekt` (every subproject) | No code smells / leftover `TODO:`/`FIXME:` comments. Overrides in `config/detekt/detekt.yml`. | Read `<module>/build/reports/detekt/detekt.txt`, fix the code (never suppress wholesale). |
| Android Lint | `lintDebug` on `:app`, `:wear`, `:core:data`, `:core:designsystem` | Lint errors (`abortOnError = true`; version-nag checks disabled). | `<module>/build/reports/lint-results-debug.html` |
| Unit, UI and screenshot tests | `:core:domain:test`, `testDebugUnitTest` on `:core:data`, `:core:designsystem`, `:app`, `:wear` | All JVM/Robolectric tests pass, every screenshot matches its golden, and the screenshot tests' accessibility checks find no errors. | `<module>/build/reports/tests/...`; for screenshots see below. |
| Architecture | `:architecture-test:test` | Konsist rules: domain purity, presentation isolation (no data layer, no Media3), ViewModel/UiState shape, `*Test` naming, design tokens only (no color or dp/sp literals in app or kit code), reference palettes read by theme code only, one font source, one Material library per app, screens build their containers from the `:core:ui` kit (`KitUsageTest`: only allowlisted Material names, Haze only in the kit's glass, no code that asks which style is on). | `architecture-test/src/test/kotlin/...` states each rule. |
| Coverage | `koverVerify` per module + root `:koverVerify` | Line **and** branch floors for every module and for the aggregate (see below). | `./gradlew :koverHtmlReport` then open `build/reports/kover/html/index.html`. |

### Coverage floors

Set in `coverageFloors` in the root `build.gradle.kts` (lines / branches, %):

| Scope | Floor |
| --- | --- |
| `:core:domain` | 96 / 92 |
| `:core:data` | 91 / 76 |
| `:core:designsystem` | 94 / 45 |
| `:core:ui` | 80 / 45 |
| `:app` | 85 / 60 |
| `:wear` | 80 / 50 |
| aggregate | 87 / 64 |

Floors sit a little under the measured values so a change can't quietly drop coverage; raise them as
tests grow. The UI modules' branch numbers include the Compose compiler's generated recomposition
branches, hence their lower branch floors.

Notes:

- One Kover setup in the root build applies to every module: generated/DI glue (`*_Factory*`,
  `Hilt_*`, `*.di.*`, R/BuildConfig, `@Preview` composables) and Android entry points (matched by
  `@AndroidEntryPoint`/`@HiltAndroidApp`, which Hilt-rewritten classes keep) are excluded.
- Coverage is measured on the debug variant; release unit tests stay uninstrumented.
- The Konsist tests read all modules' sources; Gradle doesn't know that, so if you only changed
  sources of another module, force a re-run: `./gradlew :architecture-test:test --rerun-tasks`.

## Screenshot tests

Roborazzi renders composables under Robolectric and compares them with the goldens committed in
`<module>/src/test/screenshots/` — on every test run (`roborazzi.test.verify=true` in
`gradle.properties`), so the gate fails when pixels change.

- Write one: in a Robolectric test with `@GraphicsMode(GraphicsMode.Mode.NATIVE)` and a device
  `@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)` (watch: `WearOSLargeRound` /
  `WearOSSmallRound`), call `composeRule.snapshot("feature_state") { … }` (watch:
  `wearSnapshot`). Snapshot the stateless screen or content composable; a dialog or bottom sheet
  opens its own window, which the capture doesn't see.
- The helpers also run the accessibility checks (touch targets, contrast, labels) and fail on
  errors.
- `snapshot(…, style = QandeelStyle.GLASS, tone = QandeelTone.SEPIA)` renders a look other than the
  default Mushaf light. App goldens pin glass to its unblurred tint, so they're the same on every
  machine; `:core:ui` renders through the hardware renderer (`robolectric.pixelCopyRenderMode`),
  so its kit goldens (every style × tone, and glass tinted and solid) show real blur.
- After an intended UI change, re-record and review the PNGs before committing:
  `./gradlew recordRoborazziDebug` (or `:app:recordRoborazziDebug --tests '*HomeScreenshotTest*'`).
  A failed comparison leaves compare images under `<module>/build/outputs/roborazzi/`.
- Small anti-aliasing differences between machines are tolerated (1 %). If CI's Linux rendering
  drifts further, run the CI workflow by hand with **record** checked and commit the goldens from
  its artifact.

## Pre-commit hook

The hook runs the two fast gates (`spotlessCheck detekt`) before every commit:

```
scripts/install-git-hooks.sh     # once per clone (sets git config core.hooksPath)
```

On failure it prints the fix (`./gradlew spotlessApply`); bypass with
`SKIP_QUALITY_HOOK=1 git commit ...` for WIP commits.
