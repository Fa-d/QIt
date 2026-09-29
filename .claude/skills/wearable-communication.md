# Wearable Communication

Phone → watch messaging over the Google Play Services **Wearable Data Layer** (MessageClient +
CapabilityClient). playServicesWearable 18.1.0.

## Shared message types (`:core:data/link/`)

- `WearPaths.kt` — `object WearPaths`:
  - `QURAN_DOWNLOAD = "/quran/download"` — the phone → watch message path
  - `CAPABILITY_PHONE_APP = "qandeel_phone_app"`, `CAPABILITY_WATCH_APP = "qandeel_watch_app"` — capability
    names, declared in each app's `res/values/wear.xml`
- `QuranDownloadMessage.kt` — `@Serializable data class QuranDownloadMessage(surah, trackCodes)`;
  `of(surah, tracks)` / `toBytes()` on the sender, `fromBytes(bytes)` on the receiver; `tracks`
  maps codes through `Track.fromCode` (unknown codes drop out)

## Phone side (`:app`)

- `watch/WatchConnection.kt` — the interface (`isWatchReachable()`, `sendDownload(surah, tracks):
  Result<Int>`), abstracted so ViewModels test without Play services
- `watch/WatchLink.kt` — the implementation: looks up reachable nodes by the `qandeel_watch_app`
  capability, sends the JSON payload on `/quran/download` to each (counts nodes that acknowledge ≥
  0 bytes; fails when none did; `ApiException` — no Wear OS services — means "no watch")
- `di/WatchModule.kt` binds `WatchLink` as `WatchConnection`
- Used by `SurahReaderViewModel.sendToWatch()`; the result is shown as a snackbar
  (`ReaderMessage.SentToWatch(count)` / `NoWatch`)

## Watch side (`:wear`)

- `service/QuranMessageService.kt` — `WearableListenerService` (`@AndroidEntryPoint`, injects
  `SurahDownloads`); `onMessageReceived` delegates to the top-level internal
  `handleQuranMessage(path, data, surahDownloads)` — deliberately free of wearable types so the
  payload handling is unit-tested. Bad payloads/invalid surahs/empty track lists are logged and
  dropped, never thrown; a valid message calls `surahDownloads.download(surah, tracks)`.

## Watch network (`:wear/network/WifiForDownloads.kt`)

Downloads over the phone's Bluetooth proxy crawl, so while any surah is downloading the watch
requests a Wi-Fi network (`TRANSPORT_WIFI` + `NET_CAPABILITY_INTERNET`) and binds the process to
it, releasing when downloads finish:

- `WifiRequestStateMachine` — turns download activity into edges: a rising edge (any surah starts)
  acquires once, a falling edge (none left) releases; progress updates in between are ignored
- `NetworkOps` — the Android calls, behind an interface for tests
- Started once from `WearApplication.onCreate()`

Note: `MediaSurahDownloads` starts downloads from wherever a message arrives — including the
background — so its foreground-service start falls back to a plain start and then to in-process
downloading when Android 12+ forbids it.
