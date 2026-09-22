package dev.sadakat.qit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.sadakat.qit.watch.BanglaVoiceSync
import dev.sadakat.qit.watch.ListeningSync
import javax.inject.Inject

@HiltAndroidApp
class QItApplication : Application() {

    @Inject
    lateinit var listeningSync: ListeningSync

    @Inject
    lateinit var banglaVoiceSync: BanglaVoiceSync

    override fun onCreate() {
        super.onCreate()
        listeningSync.start()
        banglaVoiceSync.start()
    }
}
