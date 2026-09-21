package dev.sadakat.qit.wear.service

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.shared.quran.audio.SurahDownloads
import javax.inject.Inject

/** Receives phone → watch requests (see [dev.sadakat.qit.shared.constants.WearPaths]). */
@AndroidEntryPoint
class QuranMessageService : WearableListenerService() {

    @Inject
    lateinit var surahDownloads: SurahDownloads

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // W4
    }
}
