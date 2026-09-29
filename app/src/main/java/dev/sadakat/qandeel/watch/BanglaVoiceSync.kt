package dev.sadakat.qandeel.watch

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qandeel.core.data.link.WearPaths
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.repository.QuranSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** The Wearable calls the Bangla voice sync makes; kept behind an interface for tests. */
interface BanglaVoiceSyncChannel {

    /** Tells the watches the Bangla is read by [voice] (with the Arabic of their recording). */
    suspend fun publishVoice(voice: BanglaVoice)
}

/** [BanglaVoiceSyncChannel] over the Wearable Data Layer. */
class WearableBanglaVoiceSyncChannel @Inject constructor(@param:ApplicationContext private val context: Context) :
    BanglaVoiceSyncChannel {

    override suspend fun publishVoice(voice: BanglaVoice) {
        val request = PutDataMapRequest.create(WearPaths.BANGLA_VOICE).run {
            dataMap.putString(WearPaths.BANGLA_VOICE_KEY, voice.code)
            asPutDataRequest()
        }
        try {
            Wearable.getDataClient(context).putDataItem(request).await()
        } catch (e: ApiException) {
            // A watch that reconnects picks the item up anyway; the next change tries again.
            Log.i(TAG, "Wearable API unavailable: ${e.statusCode}")
        }
    }
}

/**
 * Keeps the watches' Bangla voice in step with the phone's: the choice goes out as one data item
 * at [WearPaths.BANGLA_VOICE] whenever it changes — right away at start, so a watch that was away
 * gets the voice when it reconnects (a data item syncs by itself, unlike a message).
 */
@Singleton
class BanglaVoiceSync @Inject constructor(
    private val settings: QuranSettings,
    private val channel: BanglaVoiceSyncChannel,
) {

    fun start(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
        scope.launch {
            settings.banglaVoice
                .distinctUntilChanged()
                .collect { voice -> channel.publishVoice(voice) }
        }
    }
}

private const val TAG = "BanglaVoiceSync"
