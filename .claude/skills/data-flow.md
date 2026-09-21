# Data Flow

How state moves through the app. Sources are cold/hot flows from the ports; ViewModels combine
them into one `StateFlow<UiState>`; screens render it and call ViewModel functions back.

## Reader → queue → player (playback)

```
UI action (tap ayah / play surah / continue listening)
  └─ ViewModel ──> QuranPlayer.play(surah, fromAyah, mode)          [port]
        └─ ExoQuranPlayer.play                                      [adapter]
             ├─ QuranText.surah(surah)                              [port]
             ├─ QuranMediaItems.build(surah, mode)                  [:core:data]
             │     └─ QueuePlan.plan(surah, mode)                   [:core:domain]
             ├─ ExoPlayer.setMediaItems(items, startIndexOfAyah, 0)
             ├─ ExoPlayer.prepare() + play()
             └─ start MediaSessionService (intent by action)
Player.Listener events
  └─ ExoQuranPlayer.publish()
       ├─ nowPlaying: StateFlow<NowPlaying?>   (surah, ayah, track, mode, isPlaying, isBuffering)
       ├─ error: StateFlow<String?>            (network failures; cleared on resume)
       └─ savePosition → QuranSettings.saveLastPosition (once per ayah change)
ViewModels observe nowPlaying/error → UiState → Compose recomposes
```

Restore: `PlayerViewModel.init` calls `QuranPlayer.restoreLast(playWhenReady = false)`, which
reads `QuranSettings.lastPosition` and re-queues it (no-op if something is already queued).

## Downloads

```
UI action ──> ViewModel ──> SurahDownloads.download(surah, tracks)   [port]
                 └─ MediaSurahDownloads.download                     [adapter]
                      ├─ QuranAudioUrls.surahFiles(surah, track)     [:core:domain]
                      ├─ one DownloadRequest per file → QuranCache.downloadManager (Media3)
                      └─ start QuranDownloadService (foreground)
DownloadManager events / initial index load
  └─ MediaSurahDownloads: Map<fileId, FileDownloadState>
       └─ DownloadAggregation.stateOf(surah, track, files)           [:core:domain]
            └─ states: StateFlow<Map<Int, Map<Track, SurahDownloadState>>>  (surah number → track)
ViewModels combine states + mode → per-surah state via stateOf(surah, mode.tracks) → UiState
```

On the watch, the same `states` flow also drives `WifiForDownloads`
(rising/falling edges → acquire/release a Wi-Fi network).

## Settings

`QuranSettings.mode` and `.lastPosition` are cold Flows (DataStore). ViewModels combine them into
their UiState **and** keep the latest value in a field for synchronous actions (e.g.
`SurahReaderViewModel.setMode` restarts playback with the cached mode when this surah is playing).

## Phone → watch download request

```
Reader "send to watch" ──> SurahReaderViewModel.sendToWatch()
  └─ WatchConnection (interface, :app)
       └─ WatchLink: capability qit_watch_app → nodes → sendMessage(/quran/download, JSON)
            watch: QuranMessageService.onMessageReceived
              └─ handleQuranMessage → SurahDownloads.download(surah, tracks)
```

Result surfaces as a one-shot `ReaderMessage` snackbar (`SentToWatch(count)` / `NoWatch`).

## One-shot messages and errors

Transient signals (reader snackbars, player-bar errors) are a `MutableStateFlow<Message?>` **inside
the ViewModel** (never public); the UiState carries the value and the UI calls a `consumeMessage()`
/ `consumeError()` function to clear it.

## Load failures

One-shot loads use `flow { emit(...) }.catch { emit(failed = true) }` — the UiState carries a
`loadFailed`/error flag instead of throwing into Compose.
