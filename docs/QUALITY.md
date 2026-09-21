# Quality guardrails

One command proves the codebase is formatted, statically clean, lint-clean, tested, covered and
architecturally sound:

```
./gradlew qualityGate
```

Run it locally before pushing; CI (`.github/workflows/ci.yml`) runs the same command plus
`assembleDebug` on every push and pull request.

## What each gate checks

| Gate | Task(s) | What it proves | How to fix failures |
| --- | --- | --- | --- |
| Formatting | `spotlessCheck` (every subproject) | Kotlin + `.gradle.kts` follow ktlint (1.5.0, `intellij_idea` style, 120 cols) plus Compose rules (`io.nlopez.compose.rules`). Config lives in `.editorconfig`. | `./gradlew spotlessApply` |
| Static analysis | `detekt` (every subproject) | No code smells / leftover `TODO:`/`FIXME:` comments. Overrides in `config/detekt/detekt.yml`. | Read `<module>/build/reports/detekt/detekt.txt`, fix the code (never suppress wholesale). |
| Android Lint | `:app:lintDebug`, `:wear:lintDebug`, `:core:data:lintDebug` | Lint errors (`abortOnError = true`; version-nag checks disabled). | `<module>/build/reports/lint-results-debug.html` |
| Unit tests | `:core:domain:test`, `:core:data:testDebugUnitTest`, `:app:testDebugUnitTest`, `:wear:testDebugUnitTest` | All JVM/Robolectric tests pass (debug variant only for Android modules). | `<module>/build/reports/tests/...` |
| Architecture | `:architecture-test:test` | Konsist rules: domain purity, presentation isolation, ViewModel/UiState shape, `*Test` naming. | `architecture-test/src/test/kotlin/...` states each rule. |
| Coverage | `:koverVerify` (root) | Kover 0.9.1 aggregate over `:core:domain`, `:core:data`, `:app`, `:wear` (debug variants only) with a minimum of **70 %** total line coverage; `:core:domain:koverVerify` separately enforces **90 %**. | `./gradlew :koverHtmlReport` then open `build/reports/kover/html/index.html`. |

The Kover verify task behind `qualityGate` is the **root `:koverVerify`**.

Notes:

- Kover excludes generated/DI glue (`*_Factory*`, `Hilt_*`, `*.di.*`, R/BuildConfig, `@Preview`
  composables — see the `kover { }` blocks in the module build files).
- Android modules disable Kover instrumentation for `testReleaseUnitTest`, so coverage never
  triggers release unit tests.
- The Konsist tests read all modules' sources; Gradle doesn't know that, so if you only changed
  sources of another module, force a re-run: `./gradlew :architecture-test:test --rerun-tasks`.

## Pre-commit hook

The hook runs the two fast gates (`spotlessCheck detekt`) before every commit:

```
scripts/install-git-hooks.sh     # once per clone (sets git config core.hooksPath)
```

On failure it prints the fix (`./gradlew spotlessApply`); bypass with
`SKIP_QUALITY_HOOK=1 git commit ...` for WIP commits.
