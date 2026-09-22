package dev.sadakat.qit.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.sadakat.qit.wear.network.WifiForDownloads
import dev.sadakat.qit.wear.sync.BanglaVoiceSync
import dev.sadakat.qit.wear.sync.ListeningPublisher
import dev.sadakat.qit.wear.tile.TileRefresher
import javax.inject.Inject

@HiltAndroidApp
class WearApplication : Application() {

    @Inject
    lateinit var wifiForDownloads: WifiForDownloads

    @Inject
    lateinit var tileRefresher: TileRefresher

    @Inject
    lateinit var listeningPublisher: ListeningPublisher

    @Inject
    lateinit var banglaVoiceSync: BanglaVoiceSync

    override fun onCreate() {
        super.onCreate()
        wifiForDownloads.start()
        tileRefresher.start()
        listeningPublisher.start()
        banglaVoiceSync.start()
    }
}
