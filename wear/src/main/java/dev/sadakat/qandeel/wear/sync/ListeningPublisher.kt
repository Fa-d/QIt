package dev.sadakat.qandeel.wear.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qandeel.core.data.link.ListeningSnapshotMessage
import dev.sadakat.qandeel.core.data.link.WearPaths
import dev.sadakat.qandeel.core.domain.repository.ListeningHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Where the watch's listening snapshot goes; kept behind an interface for tests. */
fun interface ListeningChannel {

    /** Puts [payload] where the phone will pick it up (the [WearPaths.LISTENING] data item). */
    suspend fun publish(payload: ByteArray)
}

/** [ListeningChannel] over the Wearable Data Layer. */
class WearableListeningChannel @Inject constructor(@param:ApplicationContext private val context: Context) :
    ListeningChannel {

    override suspend fun publish(payload: ByteArray) {
        val request = PutDataMapRequest.create(WearPaths.LISTENING).run {
            dataMap.putAsset(WearPaths.LISTENING_ASSET, Asset.createFromBytes(payload))
            asPutDataRequest().setUrgent()
        }
        try {
            Wearable.getDataClient(context).putDataItem(request).await()
        } catch (e: ApiException) {
            // A watch without the Wearable API can't publish; the next change will try again.
            Log.i(TAG, "Wearable API unavailable: ${e.statusCode}")
        }
    }
}

/**
 * Publishes what this watch heard itself to the phone, so the phone's history counts it too: the
 * fresh snapshot goes out as one data item at [WearPaths.LISTENING] whenever the counts change —
 * right away at start, then debounced (one ayah's listening can write several changes). The data
 * item syncs by itself whenever the watch and phone reconnect.
 */
@Singleton
class ListeningPublisher @Inject constructor(
    private val history: ListeningHistory,
    private val channel: ListeningChannel,
) {

    @OptIn(FlowPreview::class) // debounce
    fun start(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
        scope.launch {
            publish() // Once right away, so a watch that comes back is heard from promptly.
            history.counts.debounce(PUBLISH_AFTER_MS).collect { publish() }
        }
    }

    private suspend fun publish() {
        channel.publish(ListeningSnapshotMessage.of(history.localSnapshot()).toBytes())
    }

    private companion object {
        const val PUBLISH_AFTER_MS = 10_000L
    }
}

private const val TAG = "WearableListeningChannel"
