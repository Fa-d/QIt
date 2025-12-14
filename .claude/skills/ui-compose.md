# QIt UI Framework - Jetpack Compose

## Architecture Pattern: MVVM + StateFlow

```
Compose Screen (collects from)
    ↓
StateFlow (from ViewModel)
    ↓
ViewModel (uses)
    ↓
Use Cases (uses)
    ↓
Domain Services & Repositories
```

## Phone App UI (Material3)

### Theme
**Location:** `/app/src/main/java/dev/sadakat/qit/ui/theme/`

```kotlin
@Composable
fun QItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
```

### Navigation
**Location:** `/app/src/main/java/dev/sadakat/qit/presentation/navigation/QItNavGraph.kt`

```kotlin
@Composable
fun QItNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "music_library") {
        composable("music_library") {
            MusicLibraryScreen(onNavigateToPlaylist = { navController.navigate("playlist_list") })
        }
        composable("playlist_list") {
            PlaylistListScreen()
        }
        composable("watch_sync") {
            WatchSyncScreen()
        }
    }
}
```

### Screen Structure
**Location:** `/app/src/main/java/dev/sadakat/qit/presentation/screens/`

```kotlin
@Composable
fun MusicLibraryScreen(
    viewModel: MusicLibraryViewModel = hiltViewModel(),
    onNavigateToPlaylist: () -> Unit = {}
) {
    val songs by viewModel.songs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Library") },
                actions = {
                    IconButton(onClick = { viewModel.scanMusicLibrary() }) {
                        Icon(Icons.Default.Refresh, "Scan")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToPlaylist) {
                Icon(Icons.Default.PlaylistAdd, "Create Playlist")
            }
        }
    ) { paddingValues ->
        if (isScanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(Modifier.padding(paddingValues)) {
                items(songs, key = { it.id.value }) { song ->
                    SongItem(song = song)
                }
            }
        }
    }
}
```

### Common Components
```kotlin
@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        onClick = onClick
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Cover art
            AsyncImage(
                model = song.coverArtUri,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                Text(song.artistName(), style = MaterialTheme.typography.bodyMedium)
            }
            Text(song.duration.format(), style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

## Wear App UI (Wear Compose)

### Theme
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/presentation/theme/`

```kotlin
@Composable
fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = wearColorPalette,
        typography = Typography,
        content = content
    )
}

private val wearColorPalette = Colors(
    primary = Color(0xFF6200EE),
    primaryVariant = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6),
    // ...
)
```

### Navigation
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/presentation/navigation/WearNavGraph.kt`

```kotlin
@Composable
fun WearNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "playlist_list") {
        composable("playlist_list") {
            PlaylistListScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate("song_list/${playlistId.value}")
                }
            )
        }
        composable("song_list/{playlistId}") { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId")
            SongListScreen(playlistId = PlaylistId.from(playlistId!!))
        }
        composable("playback") {
            PlaybackScreen()
        }
        composable("downloads") {
            DownloadsScreen()
        }
    }
}
```

### Wear-Specific Components

**ScalingLazyColumn** (optimized for round screens):
```kotlin
@Composable
fun PlaylistListScreen(
    viewModel: PlaylistViewModel = hiltViewModel(),
    onPlaylistClick: (PlaylistId) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        autoCentering = AutoCenteringParams(itemIndex = 0)
    ) {
        items(playlists) { playlist ->
            Chip(
                label = { Text(playlist.name) },
                onClick = { onPlaylistClick(playlist.id) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

**Chip** (touch-friendly button):
```kotlin
Chip(
    label = { Text("Download") },
    icon = { Icon(Icons.Default.CloudDownload, "Download") },
    onClick = { viewModel.downloadSong(songId) },
    colors = ChipDefaults.primaryChipColors()
)
```

**CircularProgressIndicator** (for loading):
```kotlin
CircularProgressIndicator(
    progress = downloadProgress,
    modifier = Modifier.size(48.dp),
    indicatorColor = MaterialTheme.colors.primary,
    trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
)
```

### Download Components
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/presentation/components/DownloadComponents.kt`

```kotlin
@Composable
fun DownloadStatusChip(
    downloadStatus: DownloadStatus,
    onDownloadClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (downloadStatus) {
        is DownloadStatus.NotDownloaded -> {
            Chip(
                label = { Text("Download") },
                icon = { Icon(Icons.Default.CloudDownload, "Download") },
                onClick = onDownloadClick,
                modifier = modifier
            )
        }
        is DownloadStatus.Downloading -> {
            Chip(
                label = { Text("${(downloadStatus.progress * 100).toInt()}%") },
                icon = {
                    CircularProgressIndicator(
                        progress = downloadStatus.progress,
                        modifier = Modifier.size(ChipDefaults.IconSize)
                    )
                },
                onClick = onCancelClick,
                modifier = modifier
            )
        }
        is DownloadStatus.Downloaded -> {
            Chip(
                label = { Text("Downloaded") },
                icon = { Icon(Icons.Default.CheckCircle, "Downloaded") },
                enabled = false,
                modifier = modifier
            )
        }
        // Handle Failed, Paused...
    }
}
```

## ViewModel Pattern

```kotlin
@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val scanMusicLibraryUseCase: ScanMusicLibraryUseCase,
    private val getAllSongsUseCase: GetAllSongsUseCase
) : ViewModel() {

    // UI State
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            getAllSongsUseCase().collect { songs ->
                _songs.value = songs
            }
        }
    }

    fun scanMusicLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null

            scanMusicLibraryUseCase().fold(
                onSuccess = { songs ->
                    _songs.value = songs
                },
                onFailure = { e ->
                    _error.value = e.message
                }
            )

            _isScanning.value = false
        }
    }
}
```

## State Collection in Compose

```kotlin
// Single state
val songs by viewModel.songs.collectAsState()

// Multiple states
val uiState by viewModel.uiState.collectAsState()

// With lifecycle awareness
val songs by viewModel.songs.collectAsStateWithLifecycle()
```

## Key UI Files

### Phone App
- `/app/src/main/java/dev/sadakat/qit/presentation/screens/MusicLibraryScreen.kt`
- `/app/src/main/java/dev/sadakat/qit/presentation/screens/PlaylistListScreen.kt`
- `/app/src/main/java/dev/sadakat/qit/presentation/screens/WatchSyncScreen.kt`
- `/app/src/main/java/dev/sadakat/qit/presentation/viewmodel/MusicLibraryViewModel.kt`
- `/app/src/main/java/dev/sadakat/qit/presentation/navigation/QItNavGraph.kt`

### Wear App
- `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/PlaylistListScreen.kt`
- `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/SongListScreen.kt`
- `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/PlaybackScreen.kt`
- `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/DownloadsScreen.kt`
- `/wear/src/main/java/dev/sadakat/qit/wear/presentation/viewmodel/PlaybackViewModel.kt`
- `/wear/src/main/java/dev/sadakat/qit/wear/presentation/viewmodel/DownloadViewModel.kt`
- `/wear/src/main/java/dev/sadakat/qit/wear/presentation/components/DownloadComponents.kt`
- `/wear/src/main/java/dev/sadakat/qit/wear/presentation/components/SyncStatusComponents.kt`
