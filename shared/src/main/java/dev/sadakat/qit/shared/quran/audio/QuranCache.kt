package dev.sadakat.qit.shared.quran.audio

import android.content.Context
import androidx.annotation.OptIn
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

    /** Downloaded surah audio. Never evicts: files leave only through [SurahDownloads.remove]. */
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
        Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS)
    ).apply {
        maxParallelDownloads = MAX_PARALLEL_DOWNLOADS
        requirements = Requirements(Requirements.NETWORK)
    }

    companion object {
        private const val MAX_PARALLEL_DOWNLOADS = 4

        @Volatile
        private var instance: QuranCache? = null

        fun get(context: Context): QuranCache =
            instance ?: synchronized(this) {
                instance ?: QuranCache(context.applicationContext).also { instance = it }
            }
    }
}
