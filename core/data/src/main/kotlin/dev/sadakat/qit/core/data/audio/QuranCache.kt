package dev.sadakat.qit.core.data.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.scheduler.Requirements
import java.io.File
import java.util.concurrent.Executors

/**
 * Process-wide holder of the downloaded-audio cache and its [DownloadManager].
 *
 * A singleton rather than a DI binding because [QuranDownloadService] is created by the system and
 * a [SimpleCache] directory may only be opened once per process.
 */
@OptIn(UnstableApi::class)
class QuranCache private constructor(context: Context) {

    private val databaseProvider = StandaloneDatabaseProvider(context)

    /** Threads the manager runs its download tasks on. Lazy: never needed unless downloads run. */
    private val downloadExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS)

    /** Downloaded surah audio. Never evicts: files leave only through [MediaSurahDownloads.remove]. */
    val cache = SimpleCache(File(context.filesDir, "quran_audio"), NoOpCacheEvictor(), databaseProvider)

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("QIt")
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(20_000)
        .setAllowCrossProtocolRedirects(true)

    /**
     * Data source for the player: downloaded files are read from the cache (works offline);
     * anything else streams from the network and is NOT written to the cache, so streaming never
     * masquerades as a download.
     */
    val playbackDataSourceFactory: DataSource.Factory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))
        .setCacheWriteDataSinkFactory(null)

    val downloadManager = DownloadManager(
        context,
        databaseProvider,
        cache,
        httpDataSourceFactory,
        downloadExecutor,
    ).apply {
        maxParallelDownloads = MAX_PARALLEL_DOWNLOADS
        requirements = Requirements(Requirements.NETWORK)
    }

    companion object {
        private const val MAX_PARALLEL_DOWNLOADS = 4

        @Volatile
        private var instance: QuranCache? = null

        fun get(context: Context): QuranCache = instance ?: synchronized(this) {
            instance ?: QuranCache(context.applicationContext).also { instance = it }
        }

        /**
         * Releases the singleton so the next test builds a fresh [QuranCache].
         *
         * Robolectric keeps one sandbox — one set of statics, hence one singleton — across test
         * classes while giving each of them a fresh Application. A [DownloadManager] outliving
         * its Application breaks the next class: the RequirementsWatcher receiver it registered
         * with the old Application can no longer be unregistered. Tests must therefore release
         * their instance before their class ends. Production never calls this — the singleton
         * lives as long as the process.
         */
        @VisibleForTesting
        internal fun resetForTests() {
            synchronized(this) {
                instance?.let { quranCache ->
                    quranCache.downloadManager.release()
                    quranCache.cache.release()
                    quranCache.databaseProvider.close()
                    quranCache.downloadExecutor.shutdownNow()
                }
                instance = null
            }
        }
    }
}
