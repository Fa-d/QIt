# Conflict Resolution Implementation Guide

This document describes the conflict resolution system implemented for the QIt WearOS music app to handle simultaneous modifications on phone and watch.

## Overview

The conflict resolution system provides automatic and manual strategies for resolving conflicts when the same entity (playlist or song) is modified on both the phone and watch between synchronizations.

## Architecture

### Components

1. **ConflictResolutionStrategy** (`shared/domain/valueobject/ConflictResolutionStrategy.kt`)
   - Enum defining available resolution strategies
   - Strategies: LAST_WRITE_WINS, PHONE_WINS, WATCH_WINS, MANUAL

2. **EntityType** (`shared/domain/valueobject/EntityType.kt`)
   - Enum identifying entity types that can have conflicts
   - Types: PLAYLIST, SONG, SONG_METADATA

3. **SyncConflict** (`shared/domain/valueobject/SyncConflict.kt`)
   - Value object representing a detected conflict
   - Contains both versions (phone and watch) with timestamps
   - Immutable with builder-style methods

4. **ConflictResolver** (`shared/domain/service/ConflictResolver.kt`)
   - Domain service for resolving conflicts
   - Applies resolution strategies to conflicts
   - Provides conflict detection methods

5. **SyncCoordinator** (`shared/domain/service/SyncCoordinator.kt`)
   - Enhanced with conflict detection and resolution
   - Integrates ConflictResolver into sync operations
   - Provides high-level sync methods with conflict handling

## Usage Examples

### 1. Basic Conflict Detection and Resolution

```kotlin
// Initialize SyncCoordinator (usually done via DI)
val syncCoordinator = SyncCoordinator(
    playlistRepository = playlistRepository,
    musicRepository = musicRepository,
    syncRepository = syncRepository
)

// Set desired conflict resolution strategy
syncCoordinator.setConflictResolutionStrategy(
    ConflictResolutionStrategy.LAST_WRITE_WINS
)

// Detect conflicts between phone and watch playlists
val phonePlaylists = getPhonePlaylists()
val watchPlaylists = getWatchPlaylists()
val conflicts = syncCoordinator.detectConflicts(phonePlaylists, watchPlaylists)

// Resolve and apply conflicts
val result = syncCoordinator.resolveAndApplyConflicts(conflicts)

println("Resolved: ${result.appliedCount}, Failed: ${result.failedCount}")
println("Unresolved (manual): ${result.unresolvedConflicts.size}")
```

### 2. Sync with Automatic Conflict Resolution

```kotlin
// Perform sync with conflict detection and resolution
val syncResult = syncCoordinator.syncWithConflictResolution(
    phonePlaylists = phonePlaylists,
    watchPlaylists = watchPlaylists
)

syncResult.onSuccess { result ->
    println("Playlists synced: ${result.playlistsSynced}")
    println("Songs synced: ${result.songsSynced}")
    println("Conflicts detected: ${result.conflictsDetected}")
    println("Conflicts resolved: ${result.conflictsResolved}")

    if (result.hasUnresolvedConflicts) {
        // Handle manual conflicts
        showManualConflictResolutionUI(result.unresolvedConflicts)
    }
}
```

### 3. Using Different Resolution Strategies

```kotlin
// Strategy 1: Last Write Wins (default)
syncCoordinator.setConflictResolutionStrategy(
    ConflictResolutionStrategy.LAST_WRITE_WINS
)
// Most recent timestamp wins - good for general use

// Strategy 2: Phone Always Wins
syncCoordinator.setConflictResolutionStrategy(
    ConflictResolutionStrategy.PHONE_WINS
)
// Phone is authoritative - good when phone is primary device

// Strategy 3: Watch Always Wins
syncCoordinator.setConflictResolutionStrategy(
    ConflictResolutionStrategy.WATCH_WINS
)
// Watch is authoritative - rare use case

// Strategy 4: Manual Resolution Required
syncCoordinator.setConflictResolutionStrategy(
    ConflictResolutionStrategy.MANUAL
)
// User must manually resolve - good for critical data
```

### 4. Direct Conflict Resolution with ConflictResolver

```kotlin
val conflictResolver = ConflictResolver()

// Resolve a single playlist conflict
val phonePlaylist = getPhonePlaylist(playlistId)
val watchPlaylist = getWatchPlaylist(playlistId)

val resolvedPlaylist = conflictResolver.resolvePlaylistConflict(
    phone = phonePlaylist,
    watch = watchPlaylist,
    strategy = ConflictResolutionStrategy.LAST_WRITE_WINS
)

// Save the resolved version
playlistRepository.savePlaylist(resolvedPlaylist)
syncRepository.syncPlaylistToWatch(resolvedPlaylist)
```

### 5. Checking for Conflicts Before Sync

```kotlin
val conflictResolver = ConflictResolver()

// Check if two playlists have a conflict
val hasConflict = conflictResolver.hasPlaylistConflict(
    phone = phonePlaylist,
    watch = watchPlaylist
)

if (hasConflict) {
    // Handle conflict
    val resolved = conflictResolver.resolvePlaylistConflict(
        phonePlaylist,
        watchPlaylist,
        ConflictResolutionStrategy.LAST_WRITE_WINS
    )
}
```

### 6. Manual Conflict Resolution UI Example

```kotlin
fun showManualConflictResolutionUI(conflicts: List<SyncConflict>) {
    conflicts.forEach { conflict ->
        when (conflict.entityType) {
            EntityType.PLAYLIST -> {
                val phonePlaylist = conflict.phoneVersion as Playlist
                val watchPlaylist = conflict.watchVersion as Playlist

                // Show user both versions
                AlertDialog.Builder(context)
                    .setTitle("Playlist Conflict: ${phonePlaylist.name}")
                    .setMessage(conflict.describe())
                    .setPositiveButton("Use Phone Version") { _, _ ->
                        resolveManuallyWithPhoneVersion(conflict)
                    }
                    .setNegativeButton("Use Watch Version") { _, _ ->
                        resolveManuallyWithWatchVersion(conflict)
                    }
                    .setNeutralButton("Merge") { _, _ ->
                        showMergeUI(phonePlaylist, watchPlaylist)
                    }
                    .show()
            }
            // Similar handling for SONG and SONG_METADATA
        }
    }
}
```

### 7. Batch Conflict Resolution

```kotlin
// Resolve multiple conflicts at once
val conflicts: List<SyncConflict> = detectAllConflicts()

val (resolved, unresolved) = conflictResolver.resolveConflicts(conflicts)

// Apply all resolved conflicts
resolved.forEach { conflict ->
    when (conflict.entityType) {
        EntityType.PLAYLIST -> {
            val playlist = conflict.resolvedVersion as Playlist
            playlistRepository.savePlaylist(playlist)
        }
        EntityType.SONG, EntityType.SONG_METADATA -> {
            val song = conflict.resolvedVersion as Song
            musicRepository.saveSong(song)
        }
    }
}

// Handle unresolved (MANUAL strategy)
if (unresolved.isNotEmpty()) {
    showManualConflictResolutionUI(unresolved)
}
```

## Conflict Resolution Strategies

### LAST_WRITE_WINS (Default)
- **When to use**: Most common scenario for general syncing
- **How it works**: Compares timestamps; most recent modification wins
- **Tie-breaking**: If timestamps are equal, phone version is preferred, or playlists are merged
- **Pros**: Automatic, no user intervention needed
- **Cons**: May lose intentional changes if timing is close

### PHONE_WINS
- **When to use**: Phone is the primary/authoritative device
- **How it works**: Always uses phone version, discards watch version
- **Pros**: Simple, predictable behavior
- **Cons**: Loses all watch modifications

### WATCH_WINS
- **When to use**: Watch modifications should always take precedence (rare)
- **How it works**: Always uses watch version, discards phone version
- **Pros**: Ensures watch changes are never lost
- **Cons**: Loses all phone modifications

### MANUAL
- **When to use**: Critical data or when user should decide
- **How it works**: Conflict is detected but not auto-resolved
- **Pros**: User has full control
- **Cons**: Requires user intervention, may delay sync

## Data Flow

```
1. Sync Initiated
   ↓
2. Fetch Phone Data + Watch Data
   ↓
3. Detect Conflicts
   ↓
4. Apply Resolution Strategy
   ├── Automatic (LAST_WRITE_WINS, PHONE_WINS, WATCH_WINS)
   │   ↓
   │   Resolve + Apply to Both Devices
   │
   └── Manual
       ↓
       Store Conflict + Prompt User
       ↓
       User Resolves
       ↓
       Apply to Both Devices
   ↓
5. Complete Sync
```

## Best Practices

1. **Use LAST_WRITE_WINS for most scenarios** - It's intuitive and handles most cases well

2. **Set strategy at app startup** - Configure based on user preferences or app settings

3. **Handle unresolved conflicts gracefully** - Always check for `hasUnresolvedConflicts` after sync

4. **Log conflicts for debugging** - Use `conflict.describe()` for human-readable logs

5. **Merge strategy for tie-breaks** - When timestamps are equal for playlists, the system merges songs from both versions

6. **Consider network latency** - Timestamps might be close due to network delays, not simultaneous edits

7. **User preferences** - Allow users to choose their preferred strategy in settings

## Integration with Existing Code

The conflict resolution system integrates seamlessly with existing sync infrastructure:

- Uses existing `PlaylistRepository`, `MusicRepository`, and `SyncRepository`
- Extends `SyncCoordinator` without breaking existing sync methods
- Returns enhanced results with conflict information
- Falls back gracefully when no conflicts detected

## Testing Conflict Scenarios

```kotlin
// Test scenario: Simultaneous playlist modifications
@Test
fun testPlaylistConflictResolution() {
    // Create base playlist
    val basePlaylist = Playlist.create("Test Playlist")

    // Modify on phone
    val phonePlaylist = basePlaylist.copy(
        name = "Phone Modified",
        updatedAt = System.currentTimeMillis()
    )

    // Modify on watch (slightly different time)
    val watchPlaylist = basePlaylist.copy(
        name = "Watch Modified",
        updatedAt = System.currentTimeMillis() + 1000
    )

    // Resolve with LAST_WRITE_WINS
    val resolved = conflictResolver.resolvePlaylistConflict(
        phonePlaylist,
        watchPlaylist,
        ConflictResolutionStrategy.LAST_WRITE_WINS
    )

    // Watch version should win (newer timestamp)
    assertEquals("Watch Modified", resolved.name)
}
```

## Future Enhancements

Potential improvements for the conflict resolution system:

1. **Three-way merge** - Compare with last synced version to detect concurrent modifications
2. **Conflict history** - Track and log all resolved conflicts
3. **Smart merging** - Merge non-conflicting fields even when conflict exists
4. **Conflict preview** - Show user what will change before applying resolution
5. **Undo conflicts** - Allow reverting to pre-resolution state
6. **Per-entity strategy** - Different strategies for different playlists/songs
7. **Conflict analytics** - Track conflict frequency and patterns

## Files Created

- `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/ConflictResolutionStrategy.kt`
- `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/EntityType.kt`
- `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/SyncConflict.kt`
- `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/ConflictResolver.kt`
- `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/SyncCoordinator.kt` (updated)

## Summary

The conflict resolution system provides a robust, flexible solution for handling simultaneous modifications on phone and watch. With four resolution strategies and comprehensive conflict detection, it ensures data integrity while maintaining a smooth user experience.
