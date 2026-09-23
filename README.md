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
  tap an ayah to play from there. The reciting ayah is highlighted and followed on screen, with a
  **word pointer**: the word Alafasy is reciting sits on a solid pill, words behind in full ink,
  words ahead quieter, and in long ayahs the recited line stays on screen. **Word by word** (English
  or Bangla, in the reading settings) lays each ayah out word by word with each word's meaning under
  it, lit together with the word as it is recited. Scroll away to read elsewhere and a "jump to
  reciting ayah" chip brings you back. Each heard ayah shows how often it was heard.
- **Ayah-by-ayah recitation** — three modes: Arabic only, Arabic + English, Arabic + Bangla. Surahs
  open with the basmala; the position is saved so you can continue where you left off.
- **Three Bangla voices** — in the reading settings and the player's mode menu: the Islamic
  Foundation translation with Alafasy, **Sayed Ismat Toha** with Abdul Basit (mujawwad), or
  **Shareef Baezeed Mahmood** with Sudais. Each voice plays with its own recording's reciter (and
  that reciter's word pointer); the watch follows the phone's choice.
- **Mini player and full player** — the mini player shows what plays and how far through the surah;
  swipe it up for the full player: the ayah large with the word pointer (and, word by word, the
  meaning of the word being recited) and its translation, one
  bar for the whole surah (time gone and left; drag anywhere, it names the ayah under the thumb),
  the transport with repeat and speed at its sides, and the recitation mode and sleep timer:
  - **Repeat for memorizing** — each ayah N times (or the current one forever), or a range of ayahs
    N times or forever;
  - **Speed** — 0.75× to 1.5×, natural pitch, remembered;
  - **Sleep timer** — minutes or the end of the surah, with a gentle fade-out.
- **Listening progress** — every ayah whose Arabic is heard to its end counts (skipping doesn't;
  each memorizing repeat does), on the phone and on the watch. The Progress screen (from Home)
  shows how much of the Quran has been heard and for how long, then each surah heard — by recency,
  listens or number — with its full rounds and how far into the next one; the reader shows it per
  surah and per ayah.
- **Reading settings** — Arabic text size, show/hide the translation, follow-along, theme
  (system/light/dark) and, on Android 12+, wallpaper colors.
- **Offline downloads** — download the audio files of any surah for the current mode; progress and
  failures are shown per surah, and the notification shows the whole batch ("Downloading Al-Kahf ·
  64%") and says when each surah is ready offline. Downloaded surahs play without network.
- **Notifications** — the playback notification's seek bar spans the whole surah (not one ayah's
  file) and moves across ayahs when dragged; next/previous move by ayah.
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
| Bangla (Islamic Foundation) | Alafasy Arabic + Bangla translation (split per verse) | Hugging Face dataset [`faddy001/quran_audio`](https://huggingface.co/datasets/faddy001/quran_audio) |
| Arabic (with Toha) | Abdul Basit, mujawwad, 128 kbps | `everyayah.com/data/Abdul_Basit_Mujawwad_128kbps` |
| Bangla (Toha) | Sayed Ismat Toha, Hafiz Munir Uddin Ahmad's translation (Al Quran Academy London) | the dataset, `bangla/toha-verses/` |
| Arabic (with Baezeed) | Abdur-Rahman As-Sudais, 192 kbps | `everyayah.com/data/Abdurrahmaan_As-Sudais_192kbps` |
| Bangla (Baezeed) | Shareef Baezeed Mahmood, Muhiuddin Khan's translation | the dataset, `bangla/baezeed-verses/` |

All audio is verse-by-verse, addressed by **global ayah number** (1–6236). The Bangla files are the
per-verse splits produced by `scripts/split_bangla_verses.py`; the dataset mirrors the local
`quran_audio/` folder (see `QuranAudioUrls` in `:core:domain` for the exact URLs).

The Toha and Baezeed recordings (archive.org items `Al-QuranArabicToBanglaAudio` and
`bangla-quran-audio-shareef-baezid`, fetched with `scripts/download_bangla_voices.sh`) read the
translation of a group of verses after the group's Arabic. `split_bangla_verses.py --voice toha|baezeed`
transcribes each group's translation with Bengali.AI's Whisper medium (on whisper.cpp), places its
words with forced alignment (torchaudio's MMS_FA) and matches them against six published Bangla
translations; a group is cut between two verses only where the words clearly show the boundary.
Verses it can't tell apart share one file, under the last of them, and play their translation
together after it (`SharedTranslations` in `:core:domain`, generated by `--shared-table`).
`scripts/review_bangla_split.py` prints what each file says next to a reference translation.

## Word timings and audio lengths

The word pointer uses [quran-align](https://github.com/cpfair/quran-align) by Collin Fair
(release 2016-11-24, `Alafasy_128kbps.json`, `Abdul_Basit_Mujawwad_128kbps.json` and
`Abdurrahmaan_As-Sudais_192kbps.json`), licensed
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/): word-level timings for exactly the
Alafasy files the app plays (the islamic.network files are byte-identical to the everyayah.com ones
it was aligned on). `scripts/build_audio_timing.py` maps its word indices onto the bundled text
(skipping standalone pause marks; 10:1, 13:1, 50:34 and a few unplaced words need care) and writes
`core/data/src/main/assets/quran/timing/{ar.alafasy,ar.basit-mujawwad,ar.sudais}/001..114.json`. It
also measures every Arabic, English and Bangla file with `ffprobe` over the local `quran_audio/` folder into
`assets/quran/audio/durations.json`, which lets the player treat a surah as one recording.

## Word-by-word meanings

The meaning shown under each Arabic word (English and Bangla) comes from the [quran.com](https://quran.com)
API v4 (Quran.com / Quran Foundation), whose word-by-word translations are bundled offline:
`scripts/build_word_meanings.py` fetches both languages, aligns quran.com's words onto the bundled
text (only 37:130 splits differently), and writes `core/data/src/main/assets/quran/words/en|bn/001..114.json`.

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

### Startup performance

The phone app ships a **baseline profile** (`app/src/release/generated/baselineProfiles/`): the code
it runs at startup and in its common journey (scroll the surahs, open one and read, open the full
player) is precompiled at install, and `profileinstaller` applies it on sideloaded installs too.
Regenerate it after larger UI changes, on an Android 13+ device (or a rooted one) connected over
adb — with several devices connected, pick one with `ANDROID_SERIAL`:

```bash
ANDROID_SERIAL=<phone serial> ./gradlew :app:generateBaselineProfile
```

Measure cold start with and without the profile:

```bash
ANDROID_SERIAL=<phone serial> ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
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
python3 -m unittest scripts/test_build_quran_text.py scripts/test_build_audio_timing.py -v
```

## Module map

| Module | Kind | What's in it |
| --- | --- | --- |
| `:core:domain` | pure Kotlin/JVM | Models (`QuranMeta` with juz boundaries, `Surah`, `Ayah`, `Track`, `BanglaVoice`, `RecitationMode`, `ReadingPrefs`, `AyahRefParser`, `ArabicWords`, `ListeningProgress`), pure logic (`QuranAudioUrls`, `SharedTranslations`, `QueuePlan`, `DownloadAggregation`, `WordTimings`, `SurahTimeline`, `RepeatPolicy`, `SleepTimer`, `ListenTracker`, `WordPointer`) and the ports (`QuranText`, `QuranSettings`, `SurahDownloads`, `QuranPlayer`, `AudioTimings`, `ListeningHistory`) |
| `:core:data` | Android library | Adapters for the ports: `AssetQuranText`, `DataStoreQuranSettings`, `MediaSurahDownloads`, `ExoQuranPlayer` (+ `ListeningRecorder`), `AssetAudioTimings`, `RoomListeningHistory` (Room `QuranDatabase`, schema in `core/data/schemas/`), plus `QuranCache`, `QuranDownloadService`, `QuranMediaItems` and the phone↔watch message types |
| `:core:designsystem` | Android library (Compose UI only) | Design tokens shared by phone and watch: tonal palettes, semantic colors, spacing/radius/elevation/size/motion scales, Latin and Arabic type, the Amiri Quran font, `OctagramShape` |
| `:core:testing` | pure Kotlin/JVM | Test fakes (`FakeQuranText`, `FakeSurahDownloads`, `FakeQuranSettings`, `FakeQuranPlayer`, `FakeAudioTimings`, `FakeListeningHistory`), sample data (`TestQuran`), `MainDispatcherRule` |
| `:app` | Android application | Phone UI (Compose, Material 3): home, reader, mini/full player, progress, reading settings; Hilt DI; `QuranPlaybackService`; the watch link |
| `:wear` | Android application (Wear OS) | Watch UI (Compose for Wear OS, Material 3): hub, surah/juz lists, surah, now playing, options; the tile; Hilt DI; playback + message services; Wi-Fi binding for downloads |
| `:architecture-test` | pure Kotlin/JVM | Konsist rules that enforce the architecture (domain purity, presentation isolation, ViewModel shape, design-token use, one Material per app) |
| `:baselineprofile` | Android test module | Generates the phone app's baseline profile and benchmarks its cold start on a connected device (`BaselineProfileGenerator`, `StartupBenchmark`) |

Dependencies point inward: `:app` and `:wear` → `:core:data` → `:core:domain`; both apps also use
`:core:designsystem`, which depends on nothing of ours. See
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Scripts

| Script | Purpose |
| --- | --- |
| `scripts/build_quran_text.py` | Fetch alquran.cloud editions and write the text assets (`surahs.json`, `text/001..114.json`) |
| `scripts/download_quran_audio.sh` | Download all three audio sets into `quran_audio/` (resumable) |
| `scripts/download_bangla_voices.sh` | Download the Toha and Baezeed recordings and the matching everyayah Arabic (resumable) |
| `scripts/split_bangla_verses.py` | Split the 114 Bangla surah files into per-verse mp3s by cross-correlation/DTW alignment; `--voice toha\|baezeed` for the other voices (speech recognition + forced alignment); `--shared-table` writes `SharedTranslationsData.kt` |
| `scripts/bangla_asr_split.py`, `scripts/bangla_align.py` | The recognition, alignment and cutting the extra voices' split uses (needs whisper.cpp with the Bangla model and a Python 3.12 venv with torch/torchaudio/uroman) |
| `scripts/review_bangla_split.py` | Print what each file of a split surah says next to a reference translation |
| `scripts/verify_quran_audio.sh` | Check counts, sizes and sha1 sums of the downloaded audio |
| `scripts/build_audio_timing.py` | Map quran-align's word timings onto the text and measure every audio file (`timing/<reciter>/*.json`, `audio/durations.json`) |
| `scripts/build_word_meanings.py` | Fetch quran.com's word-by-word translations and write the meaning assets (`words/en|bn/001..114.json`) |
| `scripts/test_build_quran_text.py` | Unit tests for the text builder's pure functions |
| `scripts/test_build_audio_timing.py` | Unit tests for the timing builder's pure functions |
| `scripts/test_build_word_meanings.py` | Unit tests for the meaning builder's pure functions |
| `scripts/install-git-hooks.sh` | Point git at `scripts/git-hooks/` (spotless + detekt before each commit) |
