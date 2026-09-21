# Build Config

## Toolchain
- Kotlin 2.0.0, AGP 8.7.0, Gradle 8.13; JVM target 11 (CI runs Gradle on JDK 21)
- minSdk 26, compileSdk 36 (Android modules)
- Versions centralized in `gradle/libs.versions.toml`; modules in `settings.gradle.kts`:
  `:app`, `:wear`, `:core:domain`, `:core:data`, `:core:testing`, `:architecture-test`
- Key libraries: Compose BOM 2024.12.01, Material 3 (phone) / Compose for Wear OS 1.4 (watch),
  Hilt 2.51.1, Media3 1.5.0, DataStore 1.1.1, kotlinx-serialization 1.6.3,
  play-services-wearable 18.1.0, navigation-compose 2.7.7

## Module kinds
- `:core:domain`, `:core:testing`, `:architecture-test` — `kotlin("jvm")`, no Android
- `:core:data` — Android library; merged manifest carries INTERNET/POST_NOTIFICATIONS/foreground
  permissions and declares `QuranDownloadService`
- `:app`, `:wear` — Android applications

## Common commands
```bash
./gradlew qualityGate               # spotless + detekt + lint + all tests + kover (run before pushing)
./gradlew :app:installDebug         # install phone app
./gradlew :wear:installDebug        # install watch app
./gradlew :core:domain:test         # one module's tests
./gradlew :core:data:testDebugUnitTest
./gradlew spotlessApply             # fix formatting (ktlint + compose rules)
./gradlew :koverHtmlReport          # coverage report in build/reports/kover/html/
./gradlew :architecture-test:test --rerun-tasks   # Konsist reads all modules' sources
python3 -m unittest scripts/test_build_quran_text.py -v   # text-builder tests
```

Build only the modules you need; never `clean` other modules. If the network is flaky, add
`--offline`.

## Quality gate
`./gradlew qualityGate` (root task in `build.gradle.kts`) depends on:
- `spotlessCheck` + `detekt` on every subproject — ktlint 1.5.0 (`intellij_idea` style, 120 cols,
  config in `.editorconfig`) with Compose rules (`io.nlopez.compose.rules`); detekt config in
  `config/detekt/detekt.yml` (leftover `TODO:`/`FIXME:` fail)
- `:app:lintDebug`, `:wear:lintDebug`, `:core:data:lintDebug` (abortOnError)
- Unit tests: `:core:domain:test`, `:core:data:testDebugUnitTest`, `:app:testDebugUnitTest`,
  `:wear:testDebugUnitTest`, `:architecture-test:test`
- Root `:koverVerify` — Kover 0.9.1 aggregate (≥ 70 % line coverage over `:core:domain`,
  `:core:data`, `:app`, `:wear`); `:core:domain:koverVerify` separately requires ≥ 90 %

CI (`.github/workflows/ci.yml`) runs `./gradlew qualityGate assembleDebug` on every push and PR.

## Pre-commit hook
```bash
scripts/install-git-hooks.sh    # once per clone: git config core.hooksPath scripts/git-hooks
```
Runs the fast gates (`spotlessCheck detekt`) before each commit; bypass WIP commits with
`SKIP_QUALITY_HOOK=1 git commit ...`. See `docs/QUALITY.md`.

## Test stack (per module build files)
JUnit4; `kotlinx-coroutines-test` + Turbine + `MainDispatcherRule` (from `:core:testing`);
Robolectric 4.14.1 pinned to sdk 34 via `src/test/resources/robolectric.properties`; Compose UI
tests (`createComposeRule()`); Media3 `media3-test-utils(-robolectric)` in `:core:data`
(`TestExoPlayerBuilder`, `TestPlayerRunHelper`, `FakeMediaSource`); Konsist 0.17.3 in
`:architecture-test`.

## Generated assets
The Quran text assets under `core/data/src/main/assets/quran/` are generated — never edit by hand:
```bash
python3 scripts/build_quran_text.py   # fetches alquran.cloud (quran-uthmani, en.sahih, bn.bengali)
```
