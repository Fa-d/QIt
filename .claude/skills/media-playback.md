# Media Playback and Downloads

Media3 1.5.0. Everything Android-specific lives in `:core:data`; the domain logic is in
`QueuePlan` / `QuranAudioUrls` / `DownloadAggregation` (see `domain-services.md`).

## `audio/QuranCache.kt` — process-wide audio infrastructure
A singleton (`QuranCache.get(context)`, not DI — the `QuranDownloadService` is created by the
system and a `SimpleCache` directory may only be opened once per process):

- `cache` — `SimpleCache` at `<filesDir>/quran_audio`, `NoOpCacheEvictor` (files leave only
  through `SurahDownloads.remove`)
- `playbackDataSourceFactory` — `CacheDataSource` over the cache: downloaded files play offline,
  anything else streams. **`setCacheWriteDataSinkFactory(null)`** makes streaming read-only for
  the cache, so streaming never masquerades as a download
- `downloadManager` — Media3 `DownloadManager`, 4 parallel downloads, `Requirements.NETWORK`

## `player/ExoQuranPlayer.kt` — the `QuranPlayer` port on ExoPlayer
Runs on the app-wide `ExoPlayer` with a main-thread `CoroutineScope`:

- `play(surah, fromAyah, mode)` → `QuranMediaItems.build(surah, mode)` → `setMediaItems` starting
  at `QueuePlan.indexOfAyah(...)` → `prepare()`/`play()` → starts the app's `MediaSessionService`
  (via the `androidx.media3.session.MediaSessionService` intent action) so playback and its
  notification outlive the activity
- Listener events fold into `nowPlaying: StateFlow<NowPlaying?>` (from `QueueItemId.parse` of the
  current media id) and `error: StateFlow<String?>` (`errorMessage()` maps network error codes to
  "Can't reach the audio. Check your connection or download this surah."; error clears when
  playback resumes)
- The saved position is rewritten only when the **ayah** changes, not per item
- `togglePlayPause` re-`prepare()`s when the player sits idle with a queue (after an error)
- `restoreLast(playWhenReady)` re-queues the saved position (paused by default), no-op if
  something is queued
- `sessionPlayer` — an ayah-aware `ForwardingPlayer` handed to the `MediaSession`: its
  `seekToNext`/`seekToNextMediaItem`/`seekToPrevious`/`seekToPreviousMediaItem` call
  `nextAyah()`/`previousAyah()`, so the notification's skip buttons move **by ayah**, not by track

## `audio/QuranMediaItems.kt` — queue → media items
`build(surah, mode): List<MediaItem>` from `QueuePlan.plan`: uri = the file's URL (also the cache
key), media id = `QueueItemId.toMediaId()` (`surah:ayah:trackCode`), metadata for the notification
(title "Al-Baqara 2:255" or "Al-Baqara · Bismillah", artist = Mishary Alafasy / Saheeh
International / Bangla translation, album = surah name).

## Downloads
`audio/MediaSurahDownloads.kt` implements `SurahDownloads` (see `repositories.md`):
one `DownloadRequest` per verse file into `QuranCache.downloadManager`; the pure
`DownloadAggregation` derives the per-surah states; `audio/QuranDownloadService.kt` is the
foreground `DownloadService` (channel `quran_downloads`, progress notification) declared in the
`:core:data` manifest and merged into both apps.

## MediaSession services
- Phone: `app/.../service/QuranPlaybackService.kt` — `MediaSessionService` returning the singleton
  `MediaSession` injected from `di/MediaModule.kt` (built over `ExoQuranPlayer.sessionPlayer`).
- Watch: `wear/.../service/QuranPlaybackService.kt` — builds the same kind of session over its own
  player in `onCreate`, releases it in `onDestroy`.

## ExoPlayer construction (both apps' `di/MediaModule.kt`)
`AudioAttributes` speech content type + media usage (handles audio focus), audio-becoming-noisy
handling, `DefaultMediaSourceFactory` on `QuranCache.playbackDataSourceFactory`.

## Testing
`:core:data` tests drive `ExoQuranPlayer` with `media3-test-utils-robolectric`
(`TestExoPlayerBuilder`, `TestPlayerRunHelper`, `FakeMediaSource`); the queue/aggregation
logic is tested on the JVM in `:core:domain`; ViewModels use `FakeQuranPlayer`.
