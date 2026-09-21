# Bug Fixes

Summary of bugs found during a full audit of the phone + wear + shared modules,
with the fixes applied. All modules build (`:app:assembleDebug`, `:wear:assembleDebug`).

## 1. Wearable protocol mismatches (features completely broken)

### 1.1 Download requests never worked
- **Bug:** `WearDownloadRepository.downloadSong` sent the payload as raw text
  (`"songId:QUALITY"`), but the phone's `WatchDataService.handleDownloadRequest`
  parsed it as JSON (`DownloadRequestMessage`). Parsing always failed, so no
  download could ever be started from the watch.
- **Fix:** The watch now sends a JSON `DownloadRequestMessage` (shared DTO).

### 1.2 Playback commands (watch → phone) never worked
- **Bug:** `WearSyncRepository.sendPlaybackCommand` (and the legacy
  `PhoneSyncRepository`) sent raw text (`"cmd::songId"`), but the phone parsed
  the payload as JSON (`PlaybackCommandMessage`). Every remote-control command
  from the watch was dropped.
- **Fix:** Both now send a JSON `PlaybackCommandMessage`.

### 1.3 Phone echoed playback commands back to the watch
- **Bug:** `WatchDataService.handlePlaybackCommand` forwarded the received
  command back to the watch via `syncRepository.sendPlaybackCommand`, so the
  watch's "remote control the phone" commands were never executed on the phone.
- **Fix:** The phone now executes commands locally on its own
  `PlaybackManager` via new loop-safe primitives (`playSongOnPhone`,
  `playLocal`, `pauseLocal`, `stopLocal`, `skipToNextLocal`,
  `skipToPreviousLocal`) that always act on the local player regardless of the
  playback-destination setting (preventing command ping-pong).

### 1.4 "Sync all songs" always crashed with `PlaylistId("")`
- **Bug:** `RequestSongSyncUseCase` built `PlaylistId("")` for "all songs";
  `PlaylistId` requires a non-blank value, so the require() threw every time.
- **Fix:** `SyncRepository.requestSongSyncFromPhone` now takes a nullable
  `PlaylistId?` (null = all songs). Both implementations updated.

### 1.5 Download control messages were unhandled
- **Bug:** The phone sent `/download/start|progress|complete` JSON messages;
  the watch had no handlers. Cancellation (`/download/request/cancel`) was
  likewise unhandled on the phone.
- **Fix:** New `DownloadStartMessage`/`DownloadProgressMessage`/
  `DownloadCompleteMessage` shared DTOs; watch `PhoneDataService` handles all
  three (accurate progress from the announced file size, failure cleanup);
  new `/download/cancel` path handled by the phone. The phone's start message
  moved from `/download/request` (which the watch itself uses for requests!)
  to `/download/start`.

## 2. Player / MediaSession wiring

### 2.1 Two ExoPlayer instances in both apps
- **Bug (phone):** `MediaModule` provided an `ExoPlayer` that was attached to
  the `MediaSession`, but `PlaybackManager` built its *own* player. UI playback
  happened on a player the session/notification never saw — media controls and
  the notification were dead.
- **Bug (wear):** Same split between the injected player
  (`MusicPlaybackService`) and `PlaybackManager`'s private player.
- **Fix:** Both `PlaybackManager`s now inject the app-wide `ExoPlayer` and
  attach their listeners to it. The wear player is built
  (`wear MediaModule`) with a `SchemeAwareDataSource` factory so the single
  shared player handles `streaming://`, `file://` and `content://` URIs.

### 2.2 Wear `PlaybackViewModel.onCleared()` released the singleton player
- **Bug:** Clearing the ViewModel (screen rotation) released the app-scoped
  player; the next `play()` crashed.
- **Fix:** No release in `onCleared`; player lifecycle is owned by the
  application scope. Services only release their `MediaSession` wrapper.

### 2.3 Playback services were never started
- **Bug:** Neither `MusicPlaybackService` (phone nor wear) was ever started,
  so playback died when the app went to the background and no media
  notification appeared.
- **Fix:** Both `PlaybackManager`s start their service when playback begins.

### 2.4 Broken manual media notification (phone)
- **Bug:** `MusicPlaybackService` posted a hand-built notification whose
  action PendingIntents were `null` (dead buttons), competing with Media3's
  default provider.
- **Fix:** Removed the manual notification; Media3's default media
  notification provider handles it.

## 3. Streaming & download data path

### 3.1 Download channel consumed twice on the watch
- **Bug:** `PhoneDataService.onChannelOpened` *and*
  `WearDownloadRepository`'s channel callback both read the same `/download/`
  channel input stream — two readers split the file bytes and corrupted every
  download (and used two different output directories).
- **Fix:** `PhoneDataService` no longer touches channels;
  `WearDownloadRepository` is the single consumer.

### 3.2 Duplicate stream requests corrupted audio
- **Bug:** `PlaySongUseCase` already initiated the stream (via
  `StreamingCoordinator`), then `PlaybackViewModel.playStreamedSong` sent a
  *second* request — the phone opened two channels and both were written into
  the one streaming buffer, interleaving two copies of the song.
- **Fix:** The ViewModel no longer re-requests; it just plays the already
  requested stream. `WearStreamingRepository.handleIncomingStream` also cancels
  any previous stream job as a hardening measure.

### 3.3 `StreamingAudioBuffer` was O(n²)
- **Bug:** Every read copied the entire accumulated buffer
  (`ByteArrayOutputStream.toByteArray()`) — gigabytes of memcpy per song.
- **Fix:** Rewritten as a chunk-queue buffer; reads consume chunks and only
  copy what is returned.

### 3.4 Re-sync wiped the watch's download state
- **Bug:** Synced song entities were REPLACE-inserted with
  `localFilePath = null, isDownloaded = false`, orphaning downloaded files and
  forgetting download progress on every sync.
- **Fix:** `PhoneDataService.handleSongSync` merges download-related columns
  from existing rows.

### 3.5 Watch "Downloading" list showed nothing
- **Bug:** `DownloadsScreen` filtered `downloadedSongs` (which only contains
  *completed* downloads) by active-download IDs — an empty intersection by
  construction.
- **Fix:** New `GetDownloadingQueueUseCase`; `DownloadViewModel` exposes
  `downloadingSongs` + a live progress map from the repository's queue; the
  screen renders that list.

## 4. Coroutines / hangs

- **`RoomPlaylistRepository.getPlaylistsContainingSong`** collected an
  infinite Room `Flow` → suspended forever. Now uses `first()`.
- **`WearPlaylistRepositoryImpl.getPlaylistsContainingSong`** returned an
  empty-list placeholder. Now implemented with `first()`.
- **`DownloadWorker`** collected a `StateFlow<Float>` that never completes →
  the worker ran forever. Now waits with `first { it >= 1f }` plus a 30-minute
  timeout, and the WorkManager unique-name scheme is consistent between
  enqueue/cancel paths.
- **`WearableSyncRepository.observeWatchAppStatus`** used `runBlocking` on a
  DataStore read inside a capability-change listener (jank/ANR risk). Now uses
  a `@Volatile` cached version updated asynchronously.
- **`WearSyncRepository.observeWatchConnection`** registered its listener
  *after* the initial check (events could be missed) and removed the listener
  without the capability filter. Now registers first, removes with the filter.

## 5. Misc correctness

- **Song sync chunking:** the phone now sends songs in messages of ≤200 items
  (MessageClient payloads are limited to ~100KB); large libraries previously
  failed silently.
- **Playlist-sync requests from the watch now include song metadata** (a
  playlist without its songs is useless on the watch).
- **Watch-initiated `/request/full_sync` and `/request/delta_sync`** are now
  handled by the phone (delta filters by the timestamp sent by the watch).
- **Phone playback URI** now prefers the MediaStore content `uri` over raw
  file paths (raw paths are unreliable under scoped storage on Android 10+).
- **Wear manifest:** DATA_CHANGED intent filters added for `/songs` and
  `/settings`; `POST_NOTIFICATIONS` permission added for download
  notifications on API 33+.
- **Duplicate/dead handlers removed** in watch `PhoneDataService`
  (handlePlaylistSync vs handlePlaylistDataSync etc.), and the phone
  `WatchDataService` was simplified from the EntryPoint boilerplate to plain
  `@Inject` fields.
- **Path constants:** `WearableSyncRepository` now uses `WearPaths` constants
  instead of duplicated literal strings.
- **Download request de-duplication:** the wear repository tracks
  request-sent-but-channel-not-yet-opened songs so double taps don't send two
  requests for the same song.
