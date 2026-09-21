# File Locations

Paths are relative to the repo root. Test files mirror the main packages, under `src/test/kotlin/`
(JVM modules) or `src/test/java/` (Android modules), and end in `Test`.

## Domain — `core/domain/src/main/kotlin/dev/sadakat/qit/core/domain/`
| File | Contents |
| --- | --- |
| `model/QuranMeta.kt` | `QuranMeta` — surah structure, global ayah numbering |
| `model/Surah.kt` | `Surah`, `Ayah`, `AyahRef`, `Revelation` |
| `model/Track.kt` | `Track`, `RecitationMode` |
| `audio/QuranAudioUrls.kt` | `QuranAudioUrls` (+ `AudioFile`) |
| `audio/QueuePlan.kt` | `QueuePlan`, `QueueItemId`, `QueueEntry` |
| `audio/DownloadAggregation.kt` | `DownloadAggregation`, `FileDownloadState` |
| `repository/QuranText.kt` | port |
| `repository/QuranSettings.kt` | port + `LastPosition` |
| `repository/SurahDownloads.kt` | port + `SurahDownloadState` + `stateOf` helpers |
| `player/QuranPlayer.kt` | port + `NowPlaying` |

## Data — `core/data/src/main/kotlin/dev/sadakat/qit/core/data/`
| File | Contents |
| --- | --- |
| `text/AssetQuranText.kt` | `QuranText` adapter (assets + LRU cache) |
| `text/QuranTextParser.kt` | internal JSON → domain models |
| `settings/DataStoreQuranSettings.kt` | `QuranSettings` adapter |
| `audio/QuranCache.kt` | cache + download manager singleton |
| `audio/MediaSurahDownloads.kt` | `SurahDownloads` adapter (+ internal `fileDownloadStateOf`) |
| `audio/QuranDownloadService.kt` | foreground download service |
| `audio/QuranMediaItems.kt` | queue → media items |
| `player/ExoQuranPlayer.kt` | `QuranPlayer` adapter + ayah-aware session player |
| `link/QuranDownloadMessage.kt` | phone → watch payload |
| `link/WearPaths.kt` | `/quran/download`, capability names |

Assets: `core/data/src/main/assets/quran/surahs.json` and `quran/text/001..114.json`.
Manifest (merged into both apps): `core/data/src/main/AndroidManifest.xml`.

## Phone app — `app/src/main/java/dev/sadakat/qit/`
| Path | Contents |
| --- | --- |
| `QItApplication.kt`, `MainActivity.kt` | entry points |
| `di/QuranModule.kt`, `di/MediaModule.kt`, `di/WatchModule.kt` | Hilt |
| `presentation/QuranApp.kt` | root composable (nav + player bar) |
| `presentation/navigation/QuranDestinations.kt` | routes |
| `presentation/surahlist/` | screen, route, ViewModel, `SurahSearch` |
| `presentation/reader/` | screen, route, ViewModel (`ReaderMessage`) |
| `presentation/player/` | `PlayerBar`, `PlayerViewModel` |
| `presentation/components/` | `NumberBadge`, `Titles` |
| `ui/theme/` | `Color.kt`, `Theme.kt`, `Type.kt` (incl. Amiri Quran) |
| `service/QuranPlaybackService.kt` | `MediaSessionService` |
| `watch/WatchConnection.kt`, `watch/WatchLink.kt` | watch link (interface + Wearable impl) |

## Watch app — `wear/src/main/java/dev/sadakat/qit/wear/`
| Path | Contents |
| --- | --- |
| `WearApplication.kt`, `presentation/MainActivity.kt` | entry points |
| `di/QuranModule.kt`, `di/MediaModule.kt` | Hilt |
| `presentation/WearQuranApp.kt` | root composable (SwipeDismissableNavHost) |
| `presentation/home/` | `HomeScreen`, `WearHomeViewModel` |
| `presentation/surah/` | `SurahScreen`, `WearSurahViewModel` |
| `presentation/nowplaying/` | `NowPlayingScreen`, `NowPlayingViewModel` |
| `service/QuranMessageService.kt` | receives `/quran/download` |
| `service/QuranPlaybackService.kt` | `MediaSessionService` |
| `network/WifiForDownloads.kt` | Wi-Fi while downloads run |

## Testing
- `core/testing/src/main/kotlin/dev/sadakat/qit/core/testing/` — `FakeQuranText`,
  `FakeSurahDownloads`, `FakeQuranSettings`, `FakeQuranPlayer`, `TestQuran`,
  `MainDispatcherRule`
- `architecture-test/src/test/kotlin/dev/sadakat/qit/architecture/` — `DomainIsolationTest`,
  `PresentationIsolationTest`, `ViewModelArchitectureTest`, `UiStateArchitectureTest`,
  `TestNamingArchitectureTest`
- `src/test/resources/robolectric.properties` (in `:core:data`, `:app`, `:wear`) pins sdk=34

## Scripts and config
| Path | Purpose |
| --- | --- |
| `scripts/build_quran_text.py` (+ `test_build_quran_text.py`) | build/test the text assets |
| `scripts/download_quran_audio.sh`, `verify_quran_audio.sh` | fetch/verify the audio set |
| `scripts/split_bangla_verses.py` | split Bangla surahs into verses |
| `scripts/install-git-hooks.sh`, `scripts/git-hooks/pre-commit` | quality pre-commit hook |
| `gradle/libs.versions.toml` | all dependency versions |
| `config/detekt/detekt.yml`, `.editorconfig` | static analysis / formatting |
| `docs/ARCHITECTURE.md`, `docs/QUALITY.md` | architecture & quality docs |
