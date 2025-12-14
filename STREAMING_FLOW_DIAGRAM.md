c# Streaming Playback Flow Diagram

## Complete End-to-End Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           USER INITIATES PLAYBACK                            │
│                         User taps song in library                            │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PLAYBACK VIEW MODEL                                  │
│                     playSong(songId, playlistId?)                           │
│                                                                              │
│  • Clears previous error messages                                           │
│  • Creates use case parameters                                              │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLAY SONG USE CASE                                  │
│                         invoke(params) → Result                              │
│                                                                              │
│  Step 1: Get Song from Repository                                           │
│  ┌─────────────────────────────────────────────┐                           │
│  │ musicRepository.getSongById(songId)          │                           │
│  │ → Result<Song>                               │                           │
│  └─────────────────────────────────────────────┘                           │
│                          │                                                   │
│                          ▼                                                   │
│  Step 2: Determine Streaming Strategy                                       │
│  ┌─────────────────────────────────────────────┐                           │
│  │ streamingCoordinator.determineStreamingStrategy(song)                    │
│  │                                              │                           │
│  │ Checks:                                      │                           │
│  │ • Is song available on watch?               │                           │
│  │ • Is phone connected?                       │                           │
│  │ • User streaming preference?                │                           │
│  │ • Network quality?                          │                           │
│  └─────────────────────────────────────────────┘                           │
│                          │                                                   │
│                          ▼                                                   │
│           ┌──────────────┴──────────────┐                                  │
│           │                              │                                   │
│           ▼                              ▼                                   │
│  ┌─────────────────┐          ┌──────────────────┐                         │
│  │ Strategy: Local │          │ Strategy: Stream │                         │
│  └────────┬────────┘          └────────┬─────────┘                         │
│           │                             │                                   │
│           │                             ▼                                   │
│           │              Step 3: Initiate Streaming                         │
│           │              ┌──────────────────────────────┐                  │
│           │              │ streamingCoordinator.        │                  │
│           │              │   initiateStreaming(songId)  │                  │
│           │              │                               │                  │
│           │              │ → streamingRepository.       │                  │
│           │              │     requestStreamFromPhone() │                  │
│           │              └──────────┬───────────────────┘                  │
│           │                         │                                       │
│           │                         ▼                                       │
│           │              ┌──────────────────────┐                          │
│           │              │ Success?             │                          │
│           │              └──────────┬───────────┘                          │
│           │                         │                                       │
│           │                    ┌────┴────┐                                 │
│           │                    │         │                                  │
│           │              Yes   ▼         ▼   No                            │
│           │          ┌─────────────┐ ┌──────────┐                         │
│           │          │ Continue    │ │ Return   │                         │
│           │          │             │ │ Failure  │                         │
│           │          └──────┬──────┘ └──────────┘                         │
│           │                 │                                               │
│           ▼                 ▼                                               │
│  Step 4: Return PlaybackSource                                             │
│  ┌──────────────────────────────────────────┐                             │
│  │ PlaybackSource.Local(song)                │                             │
│  │         OR                                │                             │
│  │ PlaybackSource.Streaming(song, strategy)  │                             │
│  └──────────────────────────────────────────┘                             │
│                          │                                                   │
│  Step 5: Publish Event                                                      │
│  ┌──────────────────────────────────────────┐                             │
│  │ eventPublisher.publish(                   │                             │
│  │   PlaybackStarted(songId, playlistId)     │                             │
│  │ )                                         │                             │
│  └──────────────────────────────────────────┘                             │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PLAYBACK VIEW MODEL                                  │
│                    Routes based on PlaybackSource                           │
│                                                                              │
│           ┌──────────────────┴────────────────────┐                        │
│           ▼                                        ▼                         │
│  ┌─────────────────┐                    ┌──────────────────┐              │
│  │ Local Source    │                    │ Streaming Source │              │
│  └────────┬────────┘                    └────────┬─────────┘              │
│           │                                       │                         │
│           ▼                                       ▼                         │
│  playLocalSong(song)               playStreamedSong(song, strategy)        │
└────────────────────────────────────────┬───────────────────────────────────┘
                                         │
                                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLAYBACK MANAGER                                    │
│                                                                              │
│  ┌─────────────────────────┐         ┌──────────────────────────────────┐ │
│  │ playLocalSong(song)     │         │ playStreamedSong(song, uri)      │ │
│  │                         │         │                                   │ │
│  │ 1. Set mode = Local     │         │ 1. Set mode = Streaming          │ │
│  │ 2. Get local file path  │         │ 2. Set state = Buffering(0f)     │ │
│  │ 3. Create MediaItem     │         │ 3. Create MediaItem with URI     │ │
│  │ 4. Prepare ExoPlayer    │         │ 4. Prepare ExoPlayer             │ │
│  │ 5. Start playback       │         │ 5. Start playback                │ │
│  │                         │         │ 6. Start streaming monitor       │ │
│  └─────────────────────────┘         └──────────────────────────────────┘ │
│                                                     │                        │
│                                                     ▼                        │
│                                      ┌──────────────────────────────────┐  │
│                                      │ Streaming Monitor (Coroutine)    │  │
│                                      │                                   │  │
│                                      │ Observes:                        │  │
│                                      │ streamingRepository.             │  │
│                                      │   observeStreamingStatus()       │  │
│                                      │                                   │  │
│                                      │ Updates streaming state based on:│  │
│                                      │ • Buffering progress             │  │
│                                      │ • Connection status              │  │
│                                      │ • Errors                         │  │
│                                      └──────────────────────────────────┘  │
│                                                                              │
│  ExoPlayer Listener Updates:                                                │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ STATE_IDLE      → PlaybackState.Idle                                 │ │
│  │ STATE_BUFFERING → PlaybackState.Buffering                            │ │
│  │                   StreamingPlaybackState.Buffering(progress)         │ │
│  │ STATE_READY     → PlaybackState.Ready                                │ │
│  │                   StreamingPlaybackState.Ready(song)                 │ │
│  │ STATE_ENDED     → PlaybackState.Ended                                │ │
│  │                   Stop streaming monitor                             │ │
│  │ PLAYER_ERROR    → StreamingPlaybackState.Error(message)              │ │
│  │                   handlePlaybackError()                              │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Connection Loss Scenario

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      STREAMING PLAYBACK IN PROGRESS                          │
│                   StreamingPlaybackState.Playing(song, position)            │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │ Connection Lost!       │
                    │ (Network error)        │
                    └────────┬───────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PLAYBACK MANAGER DETECTS ERROR                            │
│                                                                              │
│  Detection Sources:                                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ 1. ExoPlayer onPlayerError()                                         │ │
│  │    • ERROR_CODE_IO_NETWORK_CONNECTION_FAILED                         │ │
│  │    • ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT                        │ │
│  │                                                                       │ │
│  │ 2. StreamingRepository observeStreamingStatus()                      │ │
│  │    • StreamingStatus.Error(message)                                  │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                             │                                                │
│                             ▼                                                │
│                  handleConnectionLoss()                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ Step 1: Pause playback immediately                                   │ │
│  │         player.pause()                                               │ │
│  │                                                                       │ │
│  │ Step 2: Store last known position                                    │ │
│  │         lastKnownPosition = player.currentPosition                   │ │
│  │                                                                       │ │
│  │ Step 3: Update state                                                 │ │
│  │         streamingPlaybackState =                                     │ │
│  │           StreamingPlaybackState.Error("Connection lost")            │ │
│  │                                                                       │ │
│  │ Step 4: Stop streaming monitor                                       │ │
│  │         streamingMonitorJob?.cancel()                                │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLAYBACK VIEW MODEL                                 │
│                    Observes streaming state change                          │
│                                                                              │
│  streamingPlaybackState.collect { state ->                                  │
│    when (state) {                                                           │
│      is Error -> {                                                          │
│        errorMessage = state.message  // "Connection lost"                  │
│        isBuffering = false                                                  │
│      }                                                                      │
│    }                                                                        │
│  }                                                                          │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                   UI                                         │
│                    Shows error overlay with retry button                    │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────┐   │
│  │  ⚠️  Connection Lost                                               │   │
│  │                                                                     │   │
│  │  Unable to stream from phone. Playback has been paused.           │   │
│  │                                                                     │   │
│  │           [Dismiss]              [Retry]                          │   │
│  └────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 │ User taps [Retry]
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLAYBACK VIEW MODEL                                 │
│                          retryStreaming()                                    │
│                                                                              │
│  1. Clear error message                                                      │
│  2. Call playbackManager.retryStreaming(streamUri)                          │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLAYBACK MANAGER                                    │
│                      retryStreaming(streamUri)                              │
│                                                                              │
│  Step 1: Set buffering state                                                │
│          streamingPlaybackState = Buffering(0f)                             │
│                                                                              │
│  Step 2: Create new MediaItem                                               │
│          mediaItem = MediaItem.Builder()                                     │
│            .setUri(streamUri)                                               │
│            .build()                                                         │
│                                                                              │
│  Step 3: Restore playback state                                             │
│          player.setMediaItem(mediaItem)                                     │
│          player.prepare()                                                   │
│          player.seekTo(lastKnownPosition)  ← Resume from where it stopped  │
│          player.play()                                                      │
│                                                                              │
│  Step 4: Restart streaming monitor                                          │
│          startStreamingMonitor()                                            │
│                                                                              │
│          ┌──────────────┐                                                  │
│          │ Success?     │                                                  │
│          └──────┬───────┘                                                  │
│                 │                                                            │
│            ┌────┴────┐                                                      │
│            │         │                                                       │
│      Yes   ▼         ▼   No                                                │
│  ┌─────────────┐ ┌─────────────────┐                                      │
│  │ Playback    │ │ Error state     │                                      │
│  │ resumes     │ │ "Retry failed"  │                                      │
│  └─────────────┘ └─────────────────┘                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Buffering State Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     STREAMING PLAYBACK INITIATED                             │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
            StreamingPlaybackState.Buffering(0.0f)
                                 │
                    ┌────────────┴────────────┐
                    │                         │
                    ▼                         ▼
    ┌────────────────────────┐   ┌────────────────────────┐
    │ ExoPlayer Reports:     │   │ StreamingRepository:    │
    │ STATE_BUFFERING        │   │ StreamingStatus.        │
    │                        │   │   Buffering(progress)   │
    │ bufferedPercentage     │   │                         │
    │   = 25%                │   │ progress = 0.35f        │
    └───────────┬────────────┘   └────────┬───────────────┘
                │                         │
                └──────────┬──────────────┘
                           │
                           ▼
            StreamingPlaybackState.Buffering(0.35f)
                           │
                           ▼
                ┌──────────────────────┐
                │ PlaybackViewModel    │
                │ observes state       │
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │ isBuffering = true   │
                │ bufferingProgress =  │
                │   0.35 (35%)         │
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │ UI Updates:          │
                │ • Show spinner       │
                │ • Show "Buffering    │
                │   35%" text          │
                │ • Progress bar 35%   │
                └──────────────────────┘
                           │
                           ▼
                  Continue buffering...
                  (progress → 50% → 75% → 100%)
                           │
                           ▼
            StreamingPlaybackState.Ready(song)
                           │
                           ▼
                ┌──────────────────────┐
                │ isBuffering = false  │
                │ bufferingProgress =  │
                │   1.0 (100%)         │
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │ UI Updates:          │
                │ • Hide spinner       │
                │ • Hide buffering     │
                │   indicator          │
                │ • Show play controls │
                └──────────────────────┘
                           │
                           ▼
            StreamingPlaybackState.Playing(song, position)
```

## Mode Switching Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     CURRENTLY STREAMING FROM PHONE                           │
│                   Position: 1:45 / 3:30, Playing: true                      │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 │ User wants to switch to local
                                 │ (or automatic switch due to connection)
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLAYBACK MANAGER                                    │
│            switchPlaybackMode(song, Local, streamUri = null)                │
│                                                                              │
│  Step 1: Save current state                                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ currentPosition = player.currentPosition  // 105000ms (1:45)         │ │
│  │ wasPlaying = player.isPlaying            // true                     │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                             │                                                │
│  Step 2: Stop streaming                                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ stopStreamingMonitor()                                               │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                             │                                                │
│  Step 3: Switch to local playback                                           │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ playLocalSong(song)                                                  │ │
│  │   • Get local file path                                              │ │
│  │   • Create MediaItem from local file                                 │ │
│  │   • Set ExoPlayer media item                                         │ │
│  │   • Prepare player                                                   │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                             │                                                │
│  Step 4: Restore playback state                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ player.seekTo(currentPosition)  // Seek to 1:45                      │ │
│  │ if (wasPlaying) player.play()   // Resume playing                    │ │
│  │ else player.pause()             // Keep paused                       │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                             │                                                │
│                             ▼                                                │
│                   ✅ Seamless transition!                                   │
│                   Song continues from 1:45                                  │
│                   Playing state maintained                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

## State Exposure to UI

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLAYBACK MANAGER                                    │
│                                                                              │
│  StateFlows exposed:                                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ playbackState: StateFlow<PlaybackState>                              │ │
│  │   → Idle | Buffering | Ready | Ended                                │ │
│  │                                                                       │ │
│  │ streamingPlaybackState: StateFlow<StreamingPlaybackState>            │ │
│  │   → Idle | Buffering(progress) | Ready(song) |                      │ │
│  │      Playing(song, position) | Error(message)                       │ │
│  │                                                                       │ │
│  │ currentSong: StateFlow<Song?>                                        │ │
│  │ isPlaying: StateFlow<Boolean>                                        │ │
│  │ currentPosition: StateFlow<Long>                                     │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLAYBACK VIEW MODEL                                 │
│                                                                              │
│  Exposes to UI:                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ streamingPlaybackState: from PlaybackManager                         │ │
│  │ isPlaying: from PlaybackManager                                      │ │
│  │ playbackState: from PlaybackManager                                  │ │
│  │ currentSong: from PlaybackManager                                    │ │
│  │                                                                       │ │
│  │ isBuffering: StateFlow<Boolean>                                      │ │
│  │   → Derived from streamingPlaybackState                              │ │
│  │                                                                       │ │
│  │ bufferingProgress: StateFlow<Float>                                  │ │
│  │   → 0.0 to 1.0, from streamingPlaybackState.Buffering(progress)     │ │
│  │                                                                       │ │
│  │ errorMessage: StateFlow<String?>                                     │ │
│  │   → From streamingPlaybackState.Error(message)                       │ │
│  │                                                                       │ │
│  │ Methods:                                                             │ │
│  │   • retryStreaming()                                                 │ │
│  │   • clearError()                                                     │ │
│  │   • isStreaming()                                                    │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                   UI                                         │
│                          (Jetpack Compose)                                  │
│                                                                              │
│  Observes:                                                                   │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ val isBuffering by viewModel.isBuffering.collectAsState()           │ │
│  │ val bufferingProgress by viewModel.bufferingProgress.collectAsState()│ │
│  │ val errorMessage by viewModel.errorMessage.collectAsState()         │ │
│  │ val streamingState by viewModel.streamingPlaybackState.collectAsState()││
│  └──────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  Displays:                                                                   │
│  • Buffering indicator when isBuffering = true                              │
│  • Progress bar with bufferingProgress (0-100%)                             │
│  • Error overlay with errorMessage + retry button                          │
│  • Streaming badge when state is Playing/Buffering                         │
│  • Connection status based on streamingState                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Key Takeaways

1. **Clear Separation of Concerns**
   - Use Case handles strategy determination
   - PlaybackManager handles playback mechanics
   - ViewModel bridges to UI with convenient state

2. **Multiple State Sources**
   - ExoPlayer provides low-level playback state
   - StreamingRepository provides network state
   - Both feed into comprehensive StreamingPlaybackState

3. **Graceful Degradation**
   - Connection loss detected automatically
   - Playback paused to prevent jarring experience
   - Position saved for seamless retry
   - User given control with retry button

4. **Seamless Transitions**
   - Mode switching preserves position and play state
   - No interruption to user experience
   - Automatic fallback when needed

5. **UI Responsiveness**
   - All states exposed as StateFlows
   - UI updates automatically via Compose
   - Clear visual feedback at every stage
