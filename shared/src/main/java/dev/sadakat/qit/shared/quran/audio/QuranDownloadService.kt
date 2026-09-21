package dev.sadakat.qit.shared.quran.audio

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler

/** Foreground service that runs [QuranCache.downloadManager]. Declared in the shared manifest. */
@OptIn(UnstableApi::class)
class QuranDownloadService : DownloadService(FOREGROUND_NOTIFICATION_ID) {

    override fun getDownloadManager(): DownloadManager = QuranCache.get(this).downloadManager

    override fun getScheduler(): Scheduler? = TODO("W2a")

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification = TODO("W2a")

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 7301
    }
}
