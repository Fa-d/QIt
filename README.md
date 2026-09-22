# QIt

A Quran player for **phone and Wear OS**: browse and search all 114 surahs, read them in Arabic with
English or Bangla translation, and listen to ayah-by-ayah recitation — Arabic first, then the
translation. Surahs can be downloaded for offline listening, and the phone can ask a watch to
download them too.

## Features

Few features, each one made for listening:

- **Home** — a continue card first (surah, ayah, progress through the surah, one-tap play), then
  one search field for surah names, numbers and verse references: type `2:255` (in Western,
  Arabic-Indic or Bengali digits) to jump straight to the ayah. Browse by surah or by juz. Matching
  tolerates spelling variation (Ikhlas/Ikhlaas, Yasin/Yaseen, undiacriticed Arabic).
- **Reader** — Uthmani Arabic with Saheeh International (English) and Muhiuddin Khan (Bangla) text;
  tap an ayah to play from there. The reciting ayah is highlighted and followed on screen; scroll
  away to read elsewhere and a "jump to reciting ayah" chip brings you back.
- **Ayah-by-ayah recitation** — three modes: Arabic only, Arabic + English, Arabic + Bangla. Surahs
  open with the basmala; the position is saved so you can continue where you left off.
- **Mini player and full player** — the mini player shows what plays and how far through the surah;
  swipe it up for the full player: the ayah large with its translation, a slider to move by ayah,
  the recitation mode, and:
  - **Repeat for memorizing** — each ayah N times (or the current one forever), or a range of ayahs
    N times or forever;
  - **Speed** — 0.75× to 1.5×, natural pitch, remembered;
  - **Sleep timer** — minutes or the end of the surah, with a gentle fade-out.
- **Reading settings** — Arabic text size, show/hide the translation, follow-along, theme
  (system/light/dark) and, on Android 12+, wallpaper colors.
- **Offline downloads** — download the audio files of any surah for the current mode; progress and
  failures are shown per surah. Downloaded surahs play without network.
- **Wear OS app** — Wear Material 3: a small hub (surahs, juz, downloaded, recitation), surah
  download and play, Now playing with an ayah-progress ring and crown volume, the whole ayah text,
  and speed / repeat / sleep options. The phone's reader can send a surah download to the watch.
- **Wear OS tile** — continue listening, or see the ayah and pause, one swipe from the watch face.

The look is QIt's own "mushaf" palette — warm paper, ink, deep green and gold, with the rub el
hizb (octagram) around surah and ayah numbers — on both phone and watch, from one set of design
tokens (`:core:designsystem`).

## Audio sources

| Track | Recitation | Source |
| --- | --- | --- |
| Arabic | Mishary Alafasy, 128 kbps | `cdn.islamic.network/quran/audio/128/ar.alafasy` |
| English | Saheeh International, read by Ibrahim Walk, 192 kbps | `cdn.islamic.network/quran/audio/192/en.walk` |
| Bangla | Alafasy Arabic + Bangla translation (split per verse) | Hugging Face dataset [`faddy001/quran_audio`](https://huggingface.co/datasets/faddy001/quran_audio) |

All audio is verse-by-verse, addressed by **global ayah number** (1–6236). The Bangla files are the
per-verse splits produced by `scripts/split_bangla_verses.py`; the dataset mirrors the local
`quran_audio/` folder (see `QuranAudioUrls` in `:core:domain` for the exact URLs).

## Text sources

The bundled text assets (`core/data/src/main/assets/quran/`) are generated from the
[alquran.cloud](https://alquran.cloud) API by `scripts/build_quran_text.py`:

- `quran-uthmani` — Arabic (Uthmani script)
- `en.sahih` — English (Saheeh International)
- `bn.bengali` — Bangla (Muhiuddin Khan)

## Build and run

Requirements: Android SDK (compileSdk 37, targetSdk 36), minSdk 26. AGP 9 with built-in Kotlin,
JVM target 17. The Gradle daemon runs on JDK 21 (`gradle/gradle-daemon-jvm.properties`; Gradle
provisions it if missing).

```bash
./gradlew :app:installDebug        # install the phone app
./gradlew :wear:installDebug       # install the watch app
```

## Test and verify

One command runs formatting, static analysis, lint, all unit tests, architecture tests and the
coverage gate:

```bash
./gradlew qualityGate
```

See [docs/QUALITY.md](docs/QUALITY.md) for what each gate checks. Install the pre-commit hook once
per clone:

```bash
scripts/install-git-hooks.sh
```

The Python script `scripts/build_quran_text.py` has its own unittest suite:

```bash
python3 -m unittest scripts/test_build_quran_text.py -v
```

## Module map

| Module | Kind | What's in it |
| --- | --- | --- |
| `:core:domain` | pure Kotlin/JVM | Models (`QuranMeta` with juz boundaries, `Surah`, `Ayah`, `Track`, `RecitationMode`, `ReadingPrefs`, `AyahRefParser`), pure logic (`QuranAudioUrls`, `QueuePlan`, `DownloadAggregation`, `RepeatPolicy`, `SleepTimer`) and the ports (`QuranText`, `QuranSettings`, `SurahDownloads`, `QuranPlayer`) |
| `:core:data` | Android library | Adapters for the ports: `AssetQuranText`, `DataStoreQuranSettings`, `MediaSurahDownloads`, `ExoQuranPlayer`, plus `QuranCache`, `QuranDownloadService`, `QuranMediaItems` and the phone↔watch message types |
| `:core:designsystem` | Android library (Compose UI only) | Design tokens shared by phone and watch: tonal palettes, semantic colors, spacing/radius/elevation/size/motion scales, Latin and Arabic type, the Amiri Quran font, `OctagramShape` |
| `:core:testing` | pure Kotlin/JVM | Test fakes (`FakeQuranText`, `FakeSurahDownloads`, `FakeQuranSettings`, `FakeQuranPlayer`), sample data (`TestQuran`), `MainDispatcherRule` |
| `:app` | Android application | Phone UI (Compose, Material 3): home, reader, mini/full player, reading settings; Hilt DI; `QuranPlaybackService`; the watch link |
| `:wear` | Android application (Wear OS) | Watch UI (Compose for Wear OS, Material 3): hub, surah/juz lists, surah, now playing, options; the tile; Hilt DI; playback + message services; Wi-Fi binding for downloads |
| `:architecture-test` | pure Kotlin/JVM | Konsist rules that enforce the architecture (domain purity, presentation isolation, ViewModel shape, design-token use, one Material per app) |

Dependencies point inward: `:app` and `:wear` → `:core:data` → `:core:domain`; both apps also use
`:core:designsystem`, which depends on nothing of ours. See
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Scripts

| Script | Purpose |
| --- | --- |
| `scripts/build_quran_text.py` | Fetch alquran.cloud editions and write the text assets (`surahs.json`, `text/001..114.json`) |
| `scripts/download_quran_audio.sh` | Download all three audio sets into `quran_audio/` (resumable) |
| `scripts/split_bangla_verses.py` | Split the 114 Bangla surah files into per-verse mp3s by cross-correlation/DTW alignment |
| `scripts/verify_quran_audio.sh` | Check counts, sizes and sha1 sums of the downloaded audio |
| `scripts/test_build_quran_text.py` | Unit tests for the text builder's pure functions |
| `scripts/install-git-hooks.sh` | Point git at `scripts/git-hooks/` (spotless + detekt before each commit) |
