package dev.sadakat.qit.wear.service

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.core.data.link.QuranDownloadMessage
import dev.sadakat.qit.core.data.link.WearPaths
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import javax.inject.Inject

/** Receives phone → watch requests (see [WearPaths]). */
@AndroidEntryPoint
class QuranMessageService : WearableListenerService() {

    @Inject
    lateinit var surahDownloads: SurahDownloads

    override fun onMessageReceived(messageEvent: MessageEvent) {
        handleQuranMessage(messageEvent.path, messageEvent.data, surahDownloads)
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

private const val TAG = "QuranMessageService"
