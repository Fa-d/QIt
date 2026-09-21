# Domain Entities

All in `:core:domain`, package `dev.sadakat.qit.core.domain`. Pure Kotlin — these types must never
import Android (`DomainIsolationTest`).

## `model/QuranMeta.kt`
`object QuranMeta` — the fixed structure of the Quran:
- `SURAH_COUNT = 114`, `TOTAL_AYAHS = 6236`
- `ayahCount(surah)` — verses per surah (1..114)
- `globalAyah(surah, ayah)` — the global ayah number (1..6236) every audio source uses
- `hasBasmalaPrefix(surah)` — every surah except Al-Fatiha (1) and At-Tawbah (9)

## `model/Surah.kt`
- `enum Revelation { MECCAN, MEDINAN }`
- `data class Surah(number, nameArabic, nameEnglish, meaningEnglish, ayahCount, revelation)` —
  e.g. nameArabic "سُورَةُ ٱلْفَاتِحَةِ", nameEnglish "Al-Faatiha", meaningEnglish "The Opening";
  `globalAyah(ayah)` delegates to `QuranMeta`
- `data class Ayah(surah, number, globalNumber, arabic, english, bangla)` — Arabic is Uthmani,
  English Saheeh International, Bangla Muhiuddin Khan; `translation(track)` returns the text of a
  translation track (null for Arabic)
- `data class AyahRef(surah, ayah)` — a playback position; **ayah 0 means the basmala** before
  verse 1

## `model/Track.kt`
- `enum class Track(code, label)` — `ARABIC("ar", "Arabic")`, `ENGLISH("en", "English")`,
  `BANGLA("bn", "Bangla")`. The code is stable: used in media ids, download ids and phone→watch
  messages. `Track.fromCode(code)` is the forgiving inverse (null when unknown).
- `enum class RecitationMode(tracks, label)` — what plays for each ayah, in order:
  `ARABIC_ONLY`, `ARABIC_ENGLISH`, `ARABIC_BANGLA`. `mode.translation` is the translation track
  (null for Arabic only).

## `audio/QueuePlan.kt`
- `data class QueueItemId(surah, ayah, track)` — identity of one queue item (ayah 0 = basmala);
  `toMediaId()` → `"surah:ayah:trackCode"` (e.g. `2:255:ar`), `QueueItemId.parse(mediaId)` is the
  inverse (null for non-Quran ids)
- `data class QueueEntry(id, file: QuranAudioUrls.AudioFile)` — id plus the file to play

See `domain-services.md` for `QueuePlan`'s behaviour.

## `player/QuranPlayer.kt`
`data class NowPlaying(surah, ayah, track, mode, isPlaying, isBuffering)` — snapshot of playback;
ayah 0 while the basmala plays.

## `repository/QuranSettings.kt`
`data class LastPosition(ref: AyahRef, mode: RecitationMode)` — what "continue listening" resumes.

## `repository/SurahDownloads.kt`
`sealed interface SurahDownloadState`:
- `NotDownloaded` — the pair was never requested
- `Downloading(completedFiles, totalFiles)` — has a `progress: Float`
- `Downloaded` — every file is on disk
- `Failed(completedFiles, totalFiles)` — some files failed for good; downloading again retries

## `audio/DownloadAggregation.kt`
`enum class FileDownloadState { ACTIVE, COMPLETED, FAILED }` — state of one file, independent of
the download library.

## Test data
`:core:testing`'s `TestQuran` builds `Surah`/`Ayah` samples from the real `QuranMeta` structure
(real names for surahs 1, 2, 9, 112, 114, real ayah counts everywhere) with placeholder text.
