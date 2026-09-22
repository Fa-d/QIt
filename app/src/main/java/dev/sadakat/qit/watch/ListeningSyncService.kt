package dev.sadakat.qit.watch

import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.core.data.link.ListeningSnapshotMessage
import dev.sadakat.qit.core.data.link.WearPaths
import dev.sadakat.qit.core.domain.repository.ListeningHistory
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Receives the watches' listening items (see [WearPaths.LISTENING]): each watch keeps one data
 * item current, and its changes arrive here as CHANGED events. Runs on a background thread, so
 * blocking reads are fine.
 */
@AndroidEntryPoint
class ListeningSyncService : WearableListenerService() {

    @Inject
    lateinit var history: ListeningHistory

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        runBlocking {
            val dataClient = Wearable.getDataClient(this@ListeningSyncService)
            for (event in dataEvents) importIfListeningItem(dataClient, event)
        }
    }

    /** Imports the event's payload when it is a changed watch listening item; others are ignored. */
    private suspend fun importIfListeningItem(dataClient: DataClient, event: DataEvent) {
        val item = event.dataItem
        if (event.type != DataEvent.TYPE_CHANGED || item.uri.path != WearPaths.LISTENING) return
        val bytes = dataClient.listeningAssetOf(item) ?: return
        importListeningSnapshot(item.uri.host, bytes, history)
    }
}

/**
 * Adds what one watch heard into [history]. Kept as a top-level internal function (no wearable
 * types) so payload handling can be unit-tested. Bad payloads are logged and dropped, never
 * thrown.
 */
internal suspend fun importListeningSnapshot(nodeId: String?, bytes: ByteArray, history: ListeningHistory) {
    val message = try {
        ListeningSnapshotMessage.fromBytes(bytes)
    } catch (e: IllegalArgumentException) {
        // kotlinx.serialization's SerializationException is an IllegalArgumentException.
        Log.w(TAG, "Ignoring malformed listening snapshot from watch $nodeId", e)
        return
    }
    history.importSnapshot("watch:$nodeId", message.toSnapshot())
}

private const val TAG = "ListeningSyncService"
