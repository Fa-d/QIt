package dev.sadakat.qit.watch

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.quran.model.Track
import javax.inject.Inject
import javax.inject.Singleton

/** Talks to the QIt watch app over the Wearable Data Layer. */
@Singleton
class WatchLink @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** True if a watch with the QIt app installed is currently reachable. */
    suspend fun isWatchReachable(): Boolean = TODO("W3")

    /**
     * Asks every reachable watch to download [surah] for [tracks].
     * Returns the number of watches the request reached; fails if none did.
     */
    suspend fun sendDownload(surah: Int, tracks: List<Track>): Result<Int> = TODO("W3")
}
