package dev.sadakat.qandeel.wear.service

import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qandeel.core.data.link.ListeningResetMessage
import dev.sadakat.qandeel.core.data.link.QuranDownloadMessage
import dev.sadakat.qandeel.core.data.link.WearPaths
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.domain.repository.ListeningHistory
import dev.sadakat.qandeel.core.domain.repository.QuranSettings
import dev.sadakat.qandeel.core.domain.repository.SurahDownloads
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject

/** Receives phone → watch requests (see [WearPaths]). */
@AndroidEntryPoint
class QuranMessageService : WearableListenerService() {

    @Inject
    lateinit var surahDownloads: SurahDownloads

    @Inject
    lateinit var listeningHistory: ListeningHistory

    @Inject
    lateinit var settings: QuranSettings

    override fun onMessageReceived(messageEvent: MessageEvent) {
        handleQuranMessage(messageEvent.path, messageEvent.data, surahDownloads)
    }

    /** Runs on a background thread, so blocking reads are fine. */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val dataClient = Wearable.getDataClient(this)
        for (event in dataEvents) {
            resetIfSent(dataClient, event)
            voiceIfSent(event)
        }
    }

    /** Applies the event's payload when it is a changed listening reset; others are ignored. */
    private fun resetIfSent(dataClient: DataClient, event: DataEvent) {
        val item = event.dataItem
        if (event.type != DataEvent.TYPE_CHANGED || item.uri.path != WearPaths.LISTENING_RESET) return
        runBlocking {
            val bytes = resetBytes(dataClient, item) ?: return@runBlocking
            handleListeningReset(bytes, listeningHistory)
        }
    }

    /** Applies the event's payload when it is a changed Bangla voice; others are ignored. */
    private fun voiceIfSent(event: DataEvent) {
        val item = event.dataItem
        if (event.type != DataEvent.TYPE_CHANGED || item.uri.path != WearPaths.BANGLA_VOICE) return
        val code = DataMapItem.fromDataItem(item).dataMap.getString(WearPaths.BANGLA_VOICE_KEY).orEmpty()
        runBlocking { handleBanglaVoice(code, settings) }
    }

    /**
     * The reset item's [WearPaths.LISTENING_ASSET] payload, or null if it carries none we can read.
     * `await()` rethrows the task's own failure (an [ApiException]), unlike `Tasks.await`, which
     * wraps it in an ExecutionException.
     */
    private suspend fun resetBytes(dataClient: DataClient, item: DataItem): ByteArray? {
        val asset = DataMapItem.fromDataItem(item).dataMap.getAsset(WearPaths.LISTENING_ASSET)
        if (asset == null) {
            Log.w(TAG, "Listening reset item carries no ${WearPaths.LISTENING_ASSET} asset")
            return null
        }
        return try {
            val response = dataClient.getFdForAsset(asset).await()
            try {
                response.inputStream.readBytes()
            } finally {
                response.release()
            }
        } catch (e: IOException) {
            Log.w(TAG, "Could not read the listening reset asset", e)
            null
        } catch (e: ApiException) {
            Log.w(TAG, "Wearable API failed reading the listening reset asset", e)
            null
        }
    }
}

/**
 * Handles one phone → watch message. Kept as a top-level internal function (no wearable types)
 * so payload handling can be unit-tested. Bad payloads are logged and dropped, never thrown.
 */
internal fun handleQuranMessage(path: String, data: ByteArray, surahDownloads: SurahDownloads) {
    if (path != WearPaths.QURAN_DOWNLOAD) return
    val message = try {
        QuranDownloadMessage.fromBytes(data)
    } catch (e: IllegalArgumentException) {
        // kotlinx.serialization's SerializationException is an IllegalArgumentException.
        Log.w(TAG, "Ignoring malformed Quran download message", e)
        return
    }
    if (message.surah !in 1..QuranMeta.SURAH_COUNT) {
        Log.w(TAG, "Ignoring Quran download message for invalid surah ${message.surah}")
        return
    }
    val tracks = message.tracks
    if (tracks.isEmpty()) {
        Log.w(TAG, "Ignoring Quran download message without known tracks ${message.trackCodes}")
        return
    }
    surahDownloads.download(message.surah, tracks)
}

/**
 * Handles one phone → watch Bangla voice choice. Kept as a top-level internal function (no
 * wearable types) so payload handling can be unit-tested. An unknown code is logged and dropped,
 * never thrown.
 */
internal suspend fun handleBanglaVoice(code: String, settings: QuranSettings) {
    val voice = BanglaVoice.fromCode(code)
    if (voice == null) {
        Log.w(TAG, "Ignoring unknown Bangla voice code \"$code\"")
        return
    }
    settings.setBanglaVoice(voice)
}

/**
 * Handles one phone → watch listening reset. Kept as a top-level internal function (no wearable
 * types) so payload handling can be unit-tested. Bad payloads are logged and dropped, never
 * thrown; a reset older than ours was already applied.
 */
internal suspend fun handleListeningReset(bytes: ByteArray, history: ListeningHistory) {
    val message = try {
        ListeningResetMessage.fromBytes(bytes)
    } catch (e: IllegalArgumentException) {
        // kotlinx.serialization's SerializationException is an IllegalArgumentException.
        Log.w(TAG, "Ignoring malformed listening reset message", e)
        return
    }
    if (message.resetAt <= history.lastResetAt.first()) return
    history.reset(message.resetAt)
}

private const val TAG = "QuranMessageService"
