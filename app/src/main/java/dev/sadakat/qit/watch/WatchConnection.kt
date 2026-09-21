package dev.sadakat.qit.watch

import dev.sadakat.qit.core.domain.model.Track

/**
 * The phone's link to the QIt watch app. Abstracted so presentation ViewModels can be tested
 * without Google Play services.
 */
interface WatchConnection {

    /** True if a watch with the QIt app installed is currently reachable. */
    suspend fun isWatchReachable(): Boolean

    /**
     * Asks every reachable watch to download [surah] for [tracks].
     * Returns the number of watches the request reached; fails if none did.
     */
    suspend fun sendDownload(surah: Int, tracks: List<Track>): Result<Int>
}
