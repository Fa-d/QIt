package dev.sadakat.qandeel.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.sadakat.qandeel.wear.network.WifiForDownloads
import dev.sadakat.qandeel.wear.sync.BanglaVoiceSync
import dev.sadakat.qandeel.wear.sync.ListeningPublisher
import dev.sadakat.qandeel.wear.tile.TileRefresher
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
