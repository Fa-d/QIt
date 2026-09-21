package dev.sadakat.qit.wear.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.quran.audio.SurahDownloads
import javax.inject.Inject
import javax.inject.Singleton

/**
 * While surah downloads are running, asks for a Wi-Fi network and binds the process to it so
 * downloads do not crawl over the phone's Bluetooth proxy. Releases it when downloads finish.
 */
@Singleton
class WifiForDownloads @Inject constructor(
    @ApplicationContext private val context: Context,
    private val surahDownloads: SurahDownloads
) {

    /** Starts watching download state. Called once from [dev.sadakat.qit.wear.WearApplication]. */
    fun start() {
        // W4
    }
}
