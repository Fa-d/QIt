package dev.sadakat.qandeel

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.sadakat.qandeel.watch.BanglaVoiceSync
import dev.sadakat.qandeel.watch.ListeningSync
import javax.inject.Inject

@HiltAndroidApp
class QandeelApplication : Application() {

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
