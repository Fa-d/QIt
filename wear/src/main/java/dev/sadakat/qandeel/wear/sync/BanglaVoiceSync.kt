package dev.sadakat.qandeel.wear.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qandeel.core.data.link.WearPaths
import dev.sadakat.qandeel.core.domain.repository.QuranSettings
import dev.sadakat.qandeel.wear.service.handleBanglaVoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Where the watch reads the phone's Bangla voice choice; kept behind an interface for tests. */
fun interface BanglaVoiceSource {

    /** The phone's current voice code, or null when there is no item or none readable. */
    suspend fun voiceCode(): String?
}

/** [BanglaVoiceSource] over the Wearable Data Layer. */
class WearableBanglaVoiceSource @Inject constructor(@param:ApplicationContext private val context: Context) :
    BanglaVoiceSource {

    override suspend fun voiceCode(): String? = try {
        val dataClient = Wearable.getDataClient(context)
        dataClient.getDataItems(Uri.parse("wear://*${WearPaths.BANGLA_VOICE}")).await().use { items ->
            items.firstOrNull()?.let { item ->
                DataMapItem.fromDataItem(item).dataMap.getString(WearPaths.BANGLA_VOICE_KEY)
            }
        }
    } catch (e: ApiException) {
        // A watch whose Wear OS services are gone has no phone item to read.
        Log.i(TAG, "Wearable API unavailable: ${e.statusCode}")
        null
    }
}

/**
 * Catches this watch up on the phone's Bangla voice, once at start: changes arrive as data events
 * in [dev.sadakat.qandeel.wear.service.QuranMessageService], but a watch installed after the choice
 * was made — or away while it changed — only has the item itself, which the data layer keeps for it.
 */
@Singleton
class BanglaVoiceSync @Inject constructor(private val settings: QuranSettings, private val source: BanglaVoiceSource) {

    fun start(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
        scope.launch {
            source.voiceCode()?.let { code -> handleBanglaVoice(code, settings) }
        }
    }
}

private const val TAG = "WearableBanglaVoiceSource"
