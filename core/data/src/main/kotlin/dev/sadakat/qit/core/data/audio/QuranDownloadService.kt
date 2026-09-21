package dev.sadakat.qit.core.data.audio

import android.app.Notification
import android.app.PendingIntent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import dev.sadakat.qit.core.data.R

/** Foreground service that runs [QuranCache.downloadManager]. Declared in the :core:data manifest. */
@OptIn(UnstableApi::class)
class QuranDownloadService :
    DownloadService(
        FOREGROUND_NOTIFICATION_ID,
        DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
        CHANNEL_ID,
        R.string.quran_download_channel_name,
        0,
    ) {

    // Lazy: the Service's base context is attached after construction, so it can't be used in an initializer.
    private val notificationHelper by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

    override fun getDownloadManager(): DownloadManager = QuranCache.get(this).downloadManager

    /** The process is never killed for downloads on the devices we target, so no scheduler is needed. */
    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return notificationHelper.buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            contentIntent,
            getString(R.string.quran_download_notification_title),
            downloads,
            notMetRequirements,
        )
    }

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 7301
        private const val CHANNEL_ID = "quran_downloads"
    }
}
