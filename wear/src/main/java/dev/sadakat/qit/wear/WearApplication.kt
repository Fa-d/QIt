package dev.sadakat.qit.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.sadakat.qit.wear.network.WifiForDownloads
import javax.inject.Inject

@HiltAndroidApp
class WearApplication : Application() {

    @Inject
    lateinit var wifiForDownloads: WifiForDownloads

    override fun onCreate() {
        super.onCreate()
        wifiForDownloads.start()
    }
}
