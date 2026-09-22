package dev.sadakat.qit.core.data.audio

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.sadakat.qit.core.data.R
import dev.sadakat.qit.core.domain.audio.DownloadBatch
import kotlin.math.roundToInt

/**
 * The download notifications: one for the running batch (its whole progress, not the progress of the
 * few files in flight), and one per surah once it is done.
 */
internal object DownloadNotifications {

    const val CHANNEL_ID = "quran_downloads"
    const val PROGRESS_ID = 7301

    /** Finished-surah notifications use `FINISHED_ID_BASE + surah`, one per surah, replaced on a retry. */
    private const val FINISHED_ID_BASE = 7400
    private const val PERCENT = 100

    /** The ongoing notification of [batch]; [names] are English surah names (missing ones read "Surah N"). */
    fun progress(
        context: Context,
        batch: DownloadBatch,
        names: Map<Int, String>,
        waitingForNetwork: Boolean,
        contentIntent: PendingIntent?,
    ): Notification {
        val title = when (batch.surahs.size) {
            0 -> context.getString(R.string.quran_download_notification_title)

            1 -> context.getString(R.string.quran_download_one, name(context, names, batch.surahs.single()))

            else -> context.resources.getQuantityString(
                R.plurals.quran_download_many,
                batch.surahs.size,
                batch.surahs.size,
            )
        }
        val percent = (batch.progress * PERCENT).roundToInt()
        val text = if (waitingForNetwork) {
            context.getString(R.string.quran_download_waiting)
        } else {
            context.getString(R.string.quran_download_percent, percent)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setProgress(PERCENT, percent, batch.surahs.isEmpty())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    /** Tells that [surah] finished downloading: ready offline, or some files failed. */
    fun finished(
        context: Context,
        surah: Int,
        names: Map<Int, String>,
        failed: Boolean,
        contentIntent: PendingIntent?,
    ): Notification {
        val name = name(context, names, surah)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(
                if (failed) android.R.drawable.stat_notify_error else android.R.drawable.stat_sys_download_done,
            )
            .setContentTitle(
                context.getString(if (failed) R.string.quran_download_failed else R.string.quran_download_done, name),
            )
            .setContentText(
                context.getString(
                    if (failed) R.string.quran_download_failed_text else R.string.quran_download_done_text,
                ),
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(if (failed) NotificationCompat.CATEGORY_ERROR else NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    /** Posts [notification] for [surah]'s finish, if the app may post notifications. */
    fun postFinished(context: Context, surah: Int, notification: Notification) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(context).notify(FINISHED_ID_BASE + surah, notification)
    }

    private fun name(context: Context, names: Map<Int, String>, surah: Int) =
        names[surah] ?: context.getString(R.string.quran_download_surah_fallback, surah)
}
