package dev.sadakat.qit.wear.infrastructure.download

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for handling background downloads
 * Downloads songs even when the app is in the background
 * Shows notification with progress
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(context, workerParams) {

    private val notificationManager = NotificationManagerCompat.from(context)

    override suspend fun doWork(): Result {
        val songId = inputData.getString(KEY_SONG_ID)
            ?: return Result.failure(
                workDataOf(KEY_ERROR to "Missing song ID")
            )

        val quality = inputData.getString(KEY_QUALITY)?.let {
            try {
                AudioQuality.valueOf(it)
            } catch (e: Exception) {
                AudioQuality.MEDIUM
            }
        } ?: AudioQuality.MEDIUM

        Log.d(TAG, "Starting download worker for song: $songId, quality: $quality")

        return try {
            // Create notification channel
            createNotificationChannel()

            // Show initial notification
            showNotification(songId, 0, "Starting download...")

            // Start download
            val songIdObj = SongId.from(songId)
            val downloadResult = downloadRepository.downloadSong(songIdObj, quality)

            if (downloadResult.isFailure) {
                val error = downloadResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Download failed: $error")

                showNotification(songId, 0, "Download failed: $error", isError = true)

                return Result.failure(
                    workDataOf(KEY_ERROR to error)
                )
            }

            // Wait for the download to reach 100% (or time out).
            // Note: observing a StateFlow with collect{} would never return,
            // leaving the worker running forever - use first{} with a predicate
            // and an overall timeout instead.
            val finalProgress = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                downloadRepository.observeDownloadProgress(songIdObj)
                    .first { it >= 1.0f }
            }

            if (finalProgress == null) {
                Log.w(TAG, "Timed out waiting for download of song: $songId")
                showNotification(songId, 0, "Download timed out", isError = true)
                return Result.failure(workDataOf(KEY_ERROR to "Download timed out"))
            }

            // Show completion notification
            showNotification(songId, 100, "Download complete", isComplete = true)

            Log.d(TAG, "Download worker completed successfully for song: $songId")

            Result.success(
                workDataOf(
                    KEY_SONG_ID to songId,
                    KEY_SUCCESS to true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Download worker failed", e)

            showNotification(songId, 0, "Download failed: ${e.message}", isError = true)

            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Log.d(TAG, "Retrying download (attempt ${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(KEY_ERROR to e.message)
                )
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val songId = inputData.getString(KEY_SONG_ID) ?: "unknown"

        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading song")
            .setContentText("Preparing download...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(NOTIFICATION_ID_BASE + songId.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        // minSdk is 26, so O check is always true
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Song download notifications"
            setShowBadge(false)
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission", "NotificationPermission")
    private fun showNotification(
        songId: String,
        progress: Int,
        message: String,
        isComplete: Boolean = false,
        isError: Boolean = false
    ) {
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Notification permission not granted")
                return
            }
        }

        val notificationId = NOTIFICATION_ID_BASE + songId.hashCode()

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(if (isError) "Download failed" else "Downloading song")
            .setContentText(message)
            .setSmallIcon(
                when {
                    isError -> android.R.drawable.stat_notify_error
                    isComplete -> android.R.drawable.stat_sys_download_done
                    else -> android.R.drawable.stat_sys_download
                }
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (!isComplete && !isError) {
            builder.setProgress(100, progress, progress == 0)
                .setOngoing(true)
        } else {
            builder.setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to show notification", e)
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"

        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID_BASE = 1000

        private const val KEY_SONG_ID = "song_id"
        private const val KEY_QUALITY = "quality"
        private const val KEY_PROGRESS = "progress"
        private const val KEY_SUCCESS = "success"
        private const val KEY_ERROR = "error"

        private const val MAX_RETRY_ATTEMPTS = 3
        const val UNIQUE_WORK_PREFIX = "download_"
        private const val DOWNLOAD_TIMEOUT_MS = 30L * 60 * 1000 // 30 minutes

        /**
         * Create a OneTimeWorkRequest for downloading a song
         */
        fun createWorkRequest(
            songId: SongId,
            quality: AudioQuality = AudioQuality.MEDIUM,
            constraints: Constraints? = null
        ): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_SONG_ID to songId.value,
                KEY_QUALITY to quality.name
            )

            val constraintsToUse = constraints ?: Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .build()

            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .setConstraints(constraintsToUse)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(TAG_DOWNLOAD)
                .addTag("${TAG_DOWNLOAD}_${songId.value}")
                .build()
        }

        /**
         * Enqueue a download work request
         */
        fun enqueueDownload(
            context: Context,
            songId: SongId,
            quality: AudioQuality = AudioQuality.MEDIUM,
            constraints: Constraints? = null
        ): Operation {
            val workRequest = createWorkRequest(songId, quality, constraints)

            return WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "$UNIQUE_WORK_PREFIX${songId.value}",
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
        }

        /**
         * Cancel a download work
         */
        fun cancelDownload(context: Context, songId: SongId) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("$UNIQUE_WORK_PREFIX${songId.value}")
        }

        /**
         * Cancel all downloads
         */
        fun cancelAllDownloads(context: Context) {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(TAG_DOWNLOAD)
        }

        /**
         * Get work info for a specific download
         */
        fun getWorkInfo(context: Context, songId: SongId) =
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData("$UNIQUE_WORK_PREFIX${songId.value}")

        /**
         * Observe work info for a specific download
         */
        fun observeWorkInfo(context: Context, songId: SongId) =
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow("$UNIQUE_WORK_PREFIX${songId.value}")

        const val TAG_DOWNLOAD = "download"
    }
}

/**
 * Extension function to enqueue download with default constraints
 */
fun WorkManager.enqueueDownloadWithConstraints(
    songId: SongId,
    quality: AudioQuality = AudioQuality.MEDIUM,
    requireWifi: Boolean = false,
    requireCharging: Boolean = false
): Operation {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(
            if (requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
        )
        .setRequiresCharging(requireCharging)
        .setRequiresBatteryNotLow(true)
        .build()

    val workRequest = DownloadWorker.createWorkRequest(songId, quality, constraints)

    // Use the SAME unique-name scheme as enqueueDownload/cancelDownload,
    // otherwise cancelling would not find work enqueued through this method.
    return enqueueUniqueWork(
        "${DownloadWorker.UNIQUE_WORK_PREFIX}${songId.value}",
        ExistingWorkPolicy.KEEP,
        workRequest
    )
}
