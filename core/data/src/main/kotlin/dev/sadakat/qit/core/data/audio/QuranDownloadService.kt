package dev.sadakat.qit.core.data.audio

import android.app.Notification
import android.app.PendingIntent
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import dev.sadakat.qit.core.data.R
import dev.sadakat.qit.core.data.text.AssetQuranText
import dev.sadakat.qit.core.domain.audio.DownloadAggregation
import dev.sadakat.qit.core.domain.audio.DownloadBatch
import dev.sadakat.qit.core.domain.audio.FileDownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that runs [QuranCache.downloadManager]. Declared in the :core:data manifest.
 *
 * Every ayah is a download of its own, so the notification is built from the whole batch
 * ([DownloadAggregation.batchProgress]): "Downloading Al-Kahf · 64%", steadily up, instead of the
 * progress of the few files in flight. Each surah that finishes gets a notification of its own.
 */
@OptIn(UnstableApi::class)
class QuranDownloadService :
    DownloadService(
        DownloadNotifications.PROGRESS_ID,
        DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
        DownloadNotifications.CHANNEL_ID,
        R.string.quran_download_channel_name,
        0,
    ) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** English surah names for the notifications; "Surah N" until they are read. */
    private var surahNames: Map<Int, String> = emptyMap()

    private val finishes = SurahFinishes()

    private val listener = object : DownloadManager.Listener {
        override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) {
            val surahs = surahsOf(download.request.id)
            when (download.state) {
                Download.STATE_FAILED -> finishes.onFailed(surahs)
                Download.STATE_REMOVING -> finishes.onRemoving(surahs)
                else -> {}
            }
            announceFinished(manager)
        }

        override fun onDownloadRemoved(manager: DownloadManager, download: Download) = announceFinished(manager)
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            surahNames = runCatching { AssetQuranText(this@QuranDownloadService).surahs() }
                .getOrDefault(emptyList())
                .associate { it.number to it.nameEnglish }
        }
        downloadManager.addListener(listener)
        finishes.update(batchOf(downloadManager.currentDownloads).surahs.toSet())
    }

    override fun onDestroy() {
        downloadManager.removeListener(listener)
        scope.cancel()
        super.onDestroy()
    }

    override fun getDownloadManager(): DownloadManager = QuranCache.get(this).downloadManager

    /** The process is never killed for downloads on the devices we target, so no scheduler is needed. */
    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        DownloadNotifications.progress(
            context = this,
            batch = batchOf(downloads),
            names = surahNames,
            waitingForNetwork = notMetRequirements != 0,
            contentIntent = contentIntent(),
        )

    private fun announceFinished(manager: DownloadManager) {
        val finished = finishes.update(batchOf(manager.currentDownloads).surahs.toSet())
        for ((surah, failed) in finished) {
            val notification = DownloadNotifications.finished(this, surah, surahNames, failed, contentIntent())
            DownloadNotifications.postFinished(this, surah, notification)
        }
    }

    private fun contentIntent(): PendingIntent? = packageManager.getLaunchIntentForPackage(packageName)?.let {
        PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
    }

    internal companion object {
        /** The batch the queued and running [downloads] make up. */
        fun batchOf(downloads: List<Download>): DownloadBatch = DownloadAggregation.batchProgress(
            downloads
                .filter { fileDownloadStateOf(it) == FileDownloadState.ACTIVE }
                .associate { it.request.id to it.fraction() },
        )

        /** The surahs whose own files include [fileId] (none for a basmala shared by many). */
        fun surahsOf(fileId: String): List<Int> = DownloadAggregation.batchProgress(mapOf(fileId to 0f)).surahs

        private fun Download.fraction(): Float = if (percentDownloaded ==
            C.PERCENTAGE_UNSET.toFloat()
        ) {
            0f
        } else {
            (percentDownloaded / PERCENT).coerceIn(0f, 1f)
        }

        private const val PERCENT = 100f
    }
}
