# Domain Services (Pure Logic)

Stateless objects in `:core:domain` that hold the app's real business rules. All are plain
Kotlin/JVM and unit-tested without Android.

## `audio/QuranAudioUrls.kt` — where each verse file lives

- `AudioFile(id, url)` — `id` is the Media3 download id and is unique per file
  (`"ar/255"`, `"en/6236"`, `"bn/intro/2"`)
- `verse(track, globalAyah)`:
  - Arabic (Alafasy): `https://cdn.islamic.network/quran/audio/128/ar.alafasy/{n}.mp3`
  - English (Walk): `https://cdn.islamic.network/quran/audio/192/en.walk/{n}.mp3`
  - Bangla: `https://huggingface.co/datasets/faddy001/quran_audio/resolve/main/bangla/bangla-translation-verses/{NNNNN}.mp3`
    (dataset mirrors the local `quran_audio/` folder; `Locale.ROOT` formatting so a Bangla-locale
    device doesn't emit Bengali digits)
- `basmala(track, surah)` — the file played before verse 1; null for surahs 1 and 9. Arabic and
  English reuse verse 1 (which is the basmala); Bangla uses a per-surah intro
  (`bangla-translation-verses/intro/{NNN}.mp3`) that already contains the Arabic basmala plus its
  translation.
- `surahFiles(surah, track)` — every file needed offline: basmala (if any) + all verses.
- `HF_DATASET` is the base URL of the Hugging Face dataset.

## `audio/QueuePlan.kt` — the play queue of a surah

`QueuePlan.plan(surah, mode): List<QueueEntry>` builds the queue:

1. **Basmala prefix** (ayah 0) only when `QuranMeta.hasBasmalaPrefix`:
   - `ARABIC_ONLY` → Arabic basmala
   - `ARABIC_ENGLISH` → Arabic basmala, then English basmala
   - `ARABIC_BANGLA` → only the Bangla intro (it already contains the Arabic basmala)
2. **Verses** — ayah 1..ayahCount, each ayah once per track of the mode, in the mode's order.

Navigation indexes (operate on `List<QueueItemId>`):
- `indexOfAyah(queue, ayah)` — first item of an ayah (0 if absent)
- `nextAyahIndex(queue, currentIndex)` — first item of the next ayah; null at the end. Ayah
  numbers grow monotonically, so the first item beyond the current ayah is right regardless of
  which track is playing.
- `previousAyahIndex(queue, currentIndex, positionMs)` — restarts the current ayah when the player
  is past its first item or more than **3 s** (`RESTART_AYAH_AFTER_MS`) into it; otherwise the
  first item of the previous ayah; null at the very start.

## `audio/DownloadAggregation.kt` — per-surah state from per-file state

- `stateOf(surah, track, files: Map<String, FileDownloadState>): SurahDownloadState?` — null when
  the pair isn't tracked, i.e. none of the files **only it uses** is known. That exclusion matters:
  the Arabic/English basmala `"ar/1"`/`"en/1"` is shared by every surah's file list (and is also
  Al-Fatiha's verse 1), so downloading Al-Baqarah must not make Al-Fatiha look half-downloaded.
  - all files completed → `Downloaded`
  - any file active → `Downloading(completed, total)`
  - otherwise → `Failed(completed, total)` (missing files count as not completed)
- `pairsContaining(fileId)` — every surah/track pair a file belongs to (the shared basmala belongs
  to many; per-surah files like `"bn/intro/2"` to one).

## Presentation-side pure logic
`app/.../surahlist/SurahSearch.kt` (`internal object SurahSearch`) is UI support logic, not a
domain object: `matches(surah, rawQuery)` finds surahs however their names are spelled —
transliteration variants folded (ee→i, oo→u, doubled letters collapsed, trailing vowel+h dropped),
Arabic matched without diacritics (marks stripped, alef forms unified, ta marbuta → ha, alef
maqsura → ya), a bare number matches that surah.
