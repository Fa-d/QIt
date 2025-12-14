# QIt Wearable Communication

## Overview

QIt uses Google's Wearable Data Layer API for phone-watch communication.

## Communication Channels

### 1. DataClient (Persistent Data)
- **Purpose:** Guaranteed delivery, survives disconnection
- **Best for:** Playlists, songs (sync metadata)
- **Delivery:** Guaranteed (synced when connected)

```kotlin
// Send data
val dataClient = Wearable.getDataClient(context)
val putDataRequest = PutDataMapRequest.create("/playlists").apply {
    dataMap.putString("data", jsonString)
}.asPutDataRequest()
dataClient.putDataItem(putDataRequest)

// Receive data (in WearableListenerService)
override fun onDataChanged(dataEvents: DataEventBuffer) {
    dataEvents.forEach { event ->
        if (event.type == DataEvent.TYPE_CHANGED) {
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val jsonString = dataMap.getString("data")
            // Process data
        }
    }
}
```

### 2. MessageClient (Fire-and-Forget)
- **Purpose:** Best-effort delivery
- **Best for:** Playback commands, download requests
- **Delivery:** Not guaranteed

```kotlin
// Send message
val messageClient = Wearable.getMessageClient(context)
val nodeId = getConnectedNode() // Get watch/phone node
messageClient.sendMessage(nodeId, "/download/request", jsonBytes)

// Receive message (in WearableListenerService)
override fun onMessageReceived(messageEvent: MessageEvent) {
    when (messageEvent.path) {
        "/download/request" -> {
            val data = messageEvent.data
            // Process request
        }
    }
}
```

### 3. ChannelClient (Streaming)
- **Purpose:** Reliable bi-directional stream
- **Best for:** Audio file transfer
- **Delivery:** Reliable stream

```kotlin
// Open channel (sender)
val channelClient = Wearable.getChannelClient(context)
val channel = channelClient.openChannel(nodeId, "/stream/audio/$songId").await()
val outputStream = channelClient.getOutputStream(channel).await()

// Write audio data
outputStream.use { stream ->
    FileInputStream(audioFile).use { fis ->
        fis.copyTo(stream, bufferSize = 8192)
    }
}
channelClient.close(channel)

// Receive stream (receiver)
override fun onChannelOpened(channel: Channel) {
    val inputStream = channelClient.getInputStream(channel).await()
    inputStream.use { stream ->
        // Read and buffer audio data
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
            audioBuffer.write(buffer, 0, bytesRead)
        }
    }
}
```

### 4. CapabilityClient (Discovery)
- **Purpose:** Detect companion app
- **Best for:** Check if phone/watch app installed

```kotlin
val capabilityClient = Wearable.getCapabilityClient(context)

// Check capability
val capabilityInfo = capabilityClient.getCapability(
    "qit_watch_app",
    CapabilityClient.FILTER_REACHABLE
).await()

val connectedNodes = capabilityInfo.nodes
val isConnected = connectedNodes.isNotEmpty()

// Listen for capability changes
capabilityClient.addListener({ capabilityInfo ->
    // Handle connection change
}, "qit_watch_app")
```

## Sync Paths (WearPaths)

**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/constants/WearPaths.kt`

```kotlin
object WearPaths {
    // Data Layer paths
    const val PLAYLISTS = "/playlists"
    const val SONGS = "/songs"

    // Message paths
    const val SYNC_PLAYLISTS = "/sync/playlists"
    const val SYNC_SONGS = "/sync/songs"
    const val DOWNLOAD_REQUEST = "/download/request"
    const val DOWNLOAD_PROGRESS = "/download/progress"
    const val PLAYBACK_COMMAND = "/playback/command"

    // Channel paths
    const val STREAM_AUDIO = "/stream/audio/"  // Append songId

    // Status paths
    const val CONNECTION_STATUS = "/connection/status"
}
```

## Services

### Phone Side - WatchDataService
**Location:** `/app/src/main/java/dev/sadakat/qit/service/WatchDataService.kt`

```kotlin
class WatchDataService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                when {
                    event.dataItem.uri.path?.startsWith(WearPaths.SYNC_PLAYLISTS) == true -> {
                        // Handle sync request from watch
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearPaths.DOWNLOAD_REQUEST -> {
                // Handle download request
                val songId = String(messageEvent.data)
                // Start streaming audio
            }
        }
    }
}
```

**Manifest registration:**
```xml
<service
    android:name=".service.WatchDataService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.google.android.gms.wearable.DATA_CHANGED" />
        <action android:name="com.google.android.gms.wearable.MESSAGE_RECEIVED" />
        <data android:scheme="wear" android:host="*" android:pathPrefix="/sync" />
        <data android:scheme="wear" android:host="*" android:pathPrefix="/download" />
    </intent-filter>
</service>
```

### Watch Side - PhoneDataService
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/service/PhoneDataService.kt`

```kotlin
class PhoneDataService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                when {
                    event.dataItem.uri.path == WearPaths.PLAYLISTS -> {
                        val jsonString = dataMap.getString("data")
                        val playlists = Json.decodeFromString<List<PlaylistDto>>(jsonString)
                        // Save playlists to local database
                    }
                    event.dataItem.uri.path == WearPaths.SONGS -> {
                        val jsonString = dataMap.getString("data")
                        val songs = Json.decodeFromString<List<SongDto>>(jsonString)
                        // Save songs to local database
                    }
                }
            }
        }
    }

    override fun onChannelOpened(channel: Channel) {
        // Handle incoming audio stream
        if (channel.path.startsWith(WearPaths.STREAM_AUDIO)) {
            val songId = channel.path.removePrefix(WearPaths.STREAM_AUDIO)
            // Start buffering audio
        }
    }
}
```

**Manifest registration:**
```xml
<service
    android:name=".service.PhoneDataService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.google.android.gms.wearable.DATA_CHANGED" />
        <action android:name="com.google.android.gms.wearable.MESSAGE_RECEIVED" />
        <action android:name="com.google.android.gms.wearable.CHANNEL_EVENT" />
        <data android:scheme="wear" android:host="*" android:pathPrefix="/playlists" />
        <data android:scheme="wear" android:host="*" android:pathPrefix="/songs" />
        <data android:scheme="wear" android:host="*" android:pathPrefix="/stream" />
    </intent-filter>
</service>
```

## Repository Implementations

### WearableSyncRepository (Phone)
**Location:** `/app/src/main/java/dev/sadakat/qit/infrastructure/wearable/WearableSyncRepository.kt`

```kotlin
class WearableSyncRepository @Inject constructor(
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val channelClient: ChannelClient,
    private val nodeClient: NodeClient
) : SyncRepository {

    override suspend fun syncPlaylistsToWatch(playlists: List<Playlist>): Result<Unit> {
        return try {
            val jsonString = Json.encodeToString(playlists.map { it.toDto() })
            val putDataRequest = PutDataMapRequest.create(WearPaths.PLAYLISTS).apply {
                dataMap.putString("data", jsonString)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(putDataRequest).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isWatchConnected(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
```

### WearStreamingRepository (Watch)
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/WearStreamingRepository.kt`

Handles incoming audio streams and buffers data for ExoPlayer.

## Serialization

Uses kotlinx.serialization for JSON encoding/decoding:

```kotlin
// Serialize
val jsonString = Json.encodeToString(playlists.map { it.toDto() })

// Deserialize
val playlists = Json.decodeFromString<List<PlaylistDto>>(jsonString)
```

## Error Handling

All wearable operations should handle:
- `ApiException` - API call failures
- `TimeoutException` - Connection timeouts
- `SecurityException` - Missing permissions

```kotlin
try {
    dataClient.putDataItem(request).await()
} catch (e: ApiException) {
    Log.e(TAG, "Failed to sync: ${e.statusCode}")
} catch (e: TimeoutException) {
    Log.e(TAG, "Sync timed out")
}
```

## Connection Monitoring

```kotlin
fun observeWatchConnection(): Flow<Boolean> = callbackFlow {
    val listener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
        trySend(capabilityInfo.nodes.isNotEmpty())
    }

    capabilityClient.addListener(listener, "qit_watch_app")

    awaitClose {
        capabilityClient.removeListener(listener)
    }
}
```
