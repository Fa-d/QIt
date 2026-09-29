package dev.sadakat.qandeel.watch

import dev.sadakat.qandeel.core.domain.model.Track

/** [WatchConnection] test double: scripted reachability and result, records what was sent. */
class FakeWatchConnection : WatchConnection {

    var reachable: Boolean = true
    var sendResult: Result<Int> = Result.success(1)

    val sentSurahs = mutableListOf<Pair<Int, List<Track>>>()

    override suspend fun isWatchReachable(): Boolean = reachable

    override suspend fun sendDownload(surah: Int, tracks: List<Track>): Result<Int> {
        sentSurahs += surah to tracks
        return sendResult
    }
}
