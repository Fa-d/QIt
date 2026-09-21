package dev.sadakat.qit.watch

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.core.data.link.QuranDownloadMessage
import dev.sadakat.qit.core.data.link.WearPaths
import dev.sadakat.qit.core.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/** [WatchConnection] over the Wearable Data Layer. */
@Singleton
class WatchLink @Inject constructor(
    @ApplicationContext private val context: Context
) : WatchConnection {

    override suspend fun isWatchReachable(): Boolean = try {
        reachableNodes().isNotEmpty()
    } catch (e: ApiException) {
        // Phones without Wear OS services: no watch can ever be reachable.
        false
    }

    override suspend fun sendDownload(surah: Int, tracks: List<Track>): Result<Int> = try {
        val payload = QuranDownloadMessage.of(surah, tracks).toBytes()
        val messageClient = Wearable.getMessageClient(context)
        var sent = 0
        for (node in reachableNodes()) {
            // sendMessage returns the bytes delivered, or -1 if the node dropped the message.
            if (messageClient.sendMessage(node.id, WearPaths.QURAN_DOWNLOAD, payload).await() >= 0) sent++
        }
        if (sent == 0) Result.failure(NoWatchException) else Result.success(sent)
    } catch (e: ApiException) {
        Result.failure(e)
    }

    private suspend fun reachableNodes() = Wearable.getCapabilityClient(context)
        .getCapability(WearPaths.CAPABILITY_WATCH_APP, CapabilityClient.FILTER_REACHABLE)
        .await()
        .nodes

    private object NoWatchException : IllegalStateException("No reachable watch with the QIt app")
}
