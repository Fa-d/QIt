package dev.sadakat.qit.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.sadakat.qit.wear.network.WifiForDownloads
import dev.sadakat.qit.wear.tile.TileRefresher
import javax.inject.Inject

@HiltAndroidApp
class WearApplication : Application() {

    @Inject
    lateinit var wifiForDownloads: WifiForDownloads

    @Inject
    lateinit var tileRefresher: TileRefresher

    override fun onCreate() {
        super.onCreate()
        wifiForDownloads.start()
        tileRefresher.start()
    }
}
