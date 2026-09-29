# Qandeel Project Overview

## What is Qandeel?
Qandeel is a Quran player for phone and Wear OS, built with clean architecture (ports and adapters).
The phone app offers a surah list with search, a reader (Arabic + English/Bangla text) and
ayah-by-ayah recitation; the watch app mirrors playback and downloads. Surah audio can be
downloaded per surah for offline listening.

## Tech Stack
- Kotlin 2.0.0, AGP 8.7, Gradle 8.13; minSdk 26, compileSdk 36, JVM target 11
- Jetpack Compose (BOM 2024.12.01); Material 3 on phone, Compose for Wear OS 1.4 on watch
- Hilt 2.51, Media3 1.5.0 (ExoPlayer + session + offline downloads)
- kotlinx.serialization (phone→watch messages, asset parsing), DataStore (settings)
- JUnit4, kotlinx-coroutines-test, Turbine, Robolectric, Konsist, Kover, Spotless, Detekt

## Module Structure

```
Qandeel/
├── app/                 # Phone app (Android application)
├── wear/                # Wear OS app (Android application)
├── core/domain/         # Models, pure logic, ports (pure Kotlin/JVM)
├── core/data/           # Adapters implementing the ports (Android library)
├── core/testing/        # Fakes + sample data for tests (pure Kotlin/JVM)
├── architecture-test/   # Konsist architecture rules (pure Kotlin/JVM)
├── docs/                # ARCHITECTURE.md, QUALITY.md
└── scripts/             # Audio/text sourcing scripts, git hooks
```

## Module Responsibilities

### `:core:domain` (package `dev.sadakat.qandeel.core.domain`)
- Pure Kotlin/JVM, no Android imports (enforced by `DomainIsolationTest`)
- Models: `QuranMeta`, `Surah`, `Ayah`, `AyahRef`, `Track`, `RecitationMode`
- Pure logic: `QuranAudioUrls`, `QueuePlan`, `DownloadAggregation`
- Ports: `QuranText`, `QuranSettings`, `SurahDownloads`, `QuranPlayer`

### `:core:data` (package `dev.sadakat.qandeel.core.data`)
- Adapters: `AssetQuranText`, `DataStoreQuranSettings`, `MediaSurahDownloads`, `ExoQuranPlayer`
- Media3 plumbing: `QuranCache` (cache + download manager), `QuranDownloadService`,
  `QuranMediaItems`
- Phone→watch message: `QuranDownloadMessage`, `WearPaths`

### `:app` (package `dev.sadakat.qandeel`)
- Phone UI: surah list + search, reader, player bar (`presentation/`)
- Hilt modules (`di/`), `QuranPlaybackService`, the watch link (`watch/`)

### `:wear` (package `dev.sadakat.qandeel.wear`)
- Watch UI: home (surah list, chips), surah, now playing (`presentation/`)
- `QuranMessageService` (receives download requests), `QuranPlaybackService`
- `WifiForDownloads` (binds to Wi-Fi while downloads run)

## Audio and Text Sources
- Arabic (Alafasy 128k) and English (Saheeh Intl read by Ibrahim Walk, 192k) verse files stream
  from `cdn.islamic.network`; Bangla verse files from the Hugging Face dataset
  `faddy001/quran_audio` (mirrors the local `quran_audio/` folder produced by `scripts/`).
- Text assets (`core/data/src/main/assets/quran/`) are generated from alquran.cloud editions
  (`quran-uthmani`, `en.sahih`, `bn.bengali`) by `scripts/build_quran_text.py`.

## See Also
`docs/ARCHITECTURE.md` (full architecture), `docs/QUALITY.md` (quality gate), `README.md`.
