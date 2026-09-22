package dev.sadakat.qit.watch

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.core.data.link.ListeningResetMessage
import dev.sadakat.qit.core.data.link.WearPaths
import dev.sadakat.qit.core.domain.repository.ListeningHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** The Wearable calls the listening sync makes; kept behind an interface for tests. */
interface ListeningSyncChannel {

    /** Every watch's listening item as it stands now: the node it came from and its payload. */
    suspend fun listeningSnapshots(): List<Pair<String?, ByteArray>>

    /** Tells the watches the history was reset at [resetAt] (epoch ms). */
    suspend fun publishReset(resetAt: Long)
}

/** [ListeningSyncChannel] over the Wearable Data Layer. */
class WearableListeningSyncChannel @Inject constructor(@param:ApplicationContext private val context: Context) :
    ListeningSyncChannel {

    override suspend fun listeningSnapshots(): List<Pair<String?, ByteArray>> = try {
        val dataClient = Wearable.getDataClient(context)
        dataClient.getDataItems(Uri.parse("wear://*${WearPaths.LISTENING}")).await().use { items ->
            items.mapNotNull { item -> dataClient.listeningAssetOf(item)?.let { item.uri.host to it } }
        }
    } catch (e: ApiException) {
        // Phones without Wear OS services have no watch items to catch up on.
        Log.i(TAG, "Wearable API unavailable: ${e.statusCode}")
        emptyList()
    }

    override suspend fun publishReset(resetAt: Long) {
        val request = PutDataMapRequest.create(WearPaths.LISTENING_RESET).run {
            dataMap.putAsset(WearPaths.LISTENING_ASSET, Asset.createFromBytes(ListeningResetMessage(resetAt).toBytes()))
            asPutDataRequest().setUrgent()
        }
        try {
            Wearable.getDataClient(context).putDataItem(request).await()
        } catch (e: ApiException) {
            // A watch that reconnects picks the item up anyway; the next reset tries again.
            Log.i(TAG, "Wearable API unavailable: ${e.statusCode}")
        }
    }
}

/**
 * Keeps the phone's listening history in step with its watches: on start it imports what each
 * watch heard (the data layer holds one item per watch, so anything missed while the phone was
 * away catches up), and whenever the history is reset it tells the watches, so a watch's own
 * counts don't drag deliberately forgotten listening back in.
 */
@Singleton
class ListeningSync @Inject constructor(
    private val history: ListeningHistory,
    private val channel: ListeningSyncChannel,
) {

    fun start(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
        scope.launch {
            for ((nodeId, bytes) in channel.listeningSnapshots()) {
                importListeningSnapshot(nodeId, bytes, history)
            }
        }
        scope.launch {
            history.lastResetAt
                .filter { it > 0 } // 0 means "never reset"; there is nothing to tell the watches.
                .distinctUntilChanged()
                .collect { resetAt -> channel.publishReset(resetAt) }
        }
    }
}

/**
 * The [WearPaths.LISTENING] item's [WearPaths.LISTENING_ASSET] payload, or null if it carries
 * none we can read.
 */
internal suspend fun DataClient.listeningAssetOf(item: DataItem): ByteArray? {
    val asset = DataMapItem.fromDataItem(item).dataMap.getAsset(WearPaths.LISTENING_ASSET)
    if (asset == null) {
        Log.w(TAG, "Listening item carries no ${WearPaths.LISTENING_ASSET} asset")
        return null
    }
    return try {
        val response = getFdForAsset(asset).await()
        try {
            response.inputStream.readBytes()
        } finally {
            response.release()
        }
    } catch (e: ApiException) {
        Log.w(TAG, "Wearable API failed reading a listening item's asset", e)
        null
    } catch (e: IOException) {
        Log.w(TAG, "Could not read a listening item's asset", e)
        null
    }
}

private const val TAG = "ListeningSync"
