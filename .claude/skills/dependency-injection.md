# Dependency Injection

Hilt 2.51. Each app has its own modules; `:core:data` adapters are bound there. `:core:domain` and
`:core:testing` know nothing about Hilt.

## Phone (`app/src/main/java/dev/sadakat/qandeel/di/`)

**`QuranModule.kt`** (all `@Singleton`):
- `QuranCache` ← `QuranCache.get(context)` (singleton by process, not by DI)
- `QuranText` ← `AssetQuranText(context)`
- `QuranSettings` ← `DataStoreQuranSettings(context)`
- `SurahDownloads` ← `MediaSurahDownloads(context, quranCache)`
- `ExoQuranPlayer` ← constructed with the `ExoPlayer`, `QuranText`, `QuranSettings` and a
  main-thread `CoroutineScope` (used for queue building and position saving)
- `QuranPlayer` ← the same `ExoQuranPlayer` instance (one binding, one player)

**`MediaModule.kt`**:
- `ExoPlayer` (`@Singleton`) — speech audio attributes, audio focus, audio-becoming-noisy,
  media sources from `QuranCache.playbackDataSourceFactory`
- `MediaSession` (`@Singleton`) — over `ExoQuranPlayer.sessionPlayer` so notification
  next/previous move by ayah; injected into `service/QuranPlaybackService`

**`WatchModule.kt`** (`@Binds`): `WatchLink` as `WatchConnection`.

## Watch (`wear/src/main/java/dev/sadakat/qandeel/wear/di/`)

**`QuranModule.kt`** — same shape as the phone's: `QuranCache`, `QuranText`, `QuranSettings`,
`SurahDownloads`, `ExoQuranPlayer` (+`QuranPlayer` binding). The watch builds its `MediaSession`
itself in `service/QuranPlaybackService` (released in `onDestroy`).

**`MediaModule.kt`** — the `ExoPlayer` provider, identical settings to the phone's.

## Entry points
- `QandeelApplication` / `WearApplication` are `@HiltAndroidApp`; the watch app additionally injects
  and starts `WifiForDownloads` in `onCreate`
- `MainActivity` (both apps) is `@AndroidEntryPoint`
- Services: `QuranPlaybackService` (both apps) and `wear`'s `QuranMessageService` are
  `@AndroidEntryPoint`

## ViewModels
`@HiltViewModel` + `@Inject constructor`, resolved by `hiltViewModel()` in the `*Route`
composables. Constructors take **ports only** (`QuranText`, `QuranSettings`, `SurahDownloads`,
`QuranPlayer`; the reader also takes the app-local `WatchConnection`) plus `SavedStateHandle`.
No `Context` in ViewModel constructors (checked by `ViewModelArchitectureTest`).

## Testing
Tests construct ViewModels directly with the `:core:testing` fakes — no Hilt in unit tests.
Generated DI code is excluded from Kover coverage (see the root `build.gradle.kts` filters).
