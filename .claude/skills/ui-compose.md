# UI (Compose)

Phone: Compose + Material 3 (`:app`). Watch: Compose for Wear OS 1.4 (`:wear`). One pattern
everywhere: **unidirectional data flow**.

## Conventions (enforced)
- ViewModels: `@HiltViewModel`, one immutable `StateFlow<XxxUiState>` where `XxxUiState` is a
  data class, plain functions for user actions. No public `MutableStateFlow`, no `Context` in the
  constructor (`ViewModelArchitectureTest`, `UiStateArchitectureTest`).
- Screens: stateless composables take `(state: XxxUiState, callbacks..., modifier: Modifier =
  Modifier)`; a thin `XxxRoute` composable collects the state (`collectAsStateWithLifecycle`) and
  wires the ViewModel. Every composable that emits UI takes `modifier`.
- Presentation imports domain ports only — never `dev.sadakat.qandeel.core.data`
  (`PresentationIsolationTest`).
- State is usually built with `combine(...).stateIn(viewModelScope, WhileSubscribed(5_000), initial)`;
  one-shot loads are `flow { emit(...) }.catch { emit(failed) }`.

## Phone UI (`app/src/main/java/dev/sadakat/qandeel/presentation/`)

- `QuranApp.kt` — root: `NavHost` between surah list and reader, player bar as the `bottomBar`
  whenever something is queued, snackbar for playback errors
- `navigation/QuranDestinations.kt` — routes `surahs` and `surah/{surah}?ayah={ayah}`
- `surahlist/` — `SurahListScreen` (+`SurahListRoute`), `SurahListViewModel`
  (`SurahListUiState`: query, filtered surahs, load failed, mode, per-surah download state,
  continue-listening card), `SurahSearch` (spelling-tolerant matching, see `domain-services.md`)
- `reader/` — `SurahReaderScreen` (+`SurahReaderRoute`), `SurahReaderViewModel`
  (`SurahReaderUiState`: surah, ayahs, mode, download state, playing ayah, initial scroll ayah,
  one-shot `ReaderMessage` snackbars). Actions: play ayah/surah, download/remove, change mode
  (restarts playback at the current ayah if this surah is playing), send to watch
- `player/` — `PlayerBar` + `PlayerViewModel` (`PlayerBarUiState`: now playing, surah name,
  error). `init` calls `player.restoreLast(playWhenReady = false)` so the bar reappears after an
  app restart; dismissed errors are suppressed until a new one arrives
- `components/` — `NumberBadge.kt` and `Titles.kt` (small shared pieces: the surah-number badge
  composable and the player-bar title text helpers)
- `ui/theme/` — Material 3 theme, colors, types (Amiri Quran typeface for Arabic;
  `AmiriQuran` is used by the reader)

## Watch UI (`wear/src/main/java/dev/sadakat/qandeel/wear/presentation/`)

- `WearQuranApp.kt` — root: `SwipeDismissableNavHost` over routes `home`, `surah/{number}`,
  `nowplaying`
- `home/` — `HomeScreen` (+`HomeRoute`), `WearHomeViewModel` (`UiState`: surah rows with download
  state, mode, now-playing chip, continue chip). `cycleMode()` steps
  Arabic-only → Arabic+English → Arabic+Bangla; `continuePlaying()` restores the saved position
- `surah/` — `SurahScreen` (+`SurahRoute`), `WearSurahViewModel` (`UiState`: surah, mode,
  download state; actions play/download/remove)
- `nowplaying/` — `NowPlayingScreen` (+`NowPlayingRoute`), `NowPlayingViewModel` (`UiState`:
  surah name, current ayah and its Arabic text, play/pause/buffering, error). Ayah 0 (basmala)
  has no `QuranText` entry; the screen shows the basmala itself.

## Testing UI
Compose UI tests on Robolectric (`createComposeRule()`) drive the stateless screens directly with
hand-built `UiState`s; ViewModel tests use the `:core:testing` fakes with `runTest`, Turbine and
`MainDispatcherRule`. Test files mirror the main packages (`src/test/java/`, Android modules).
