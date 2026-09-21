package dev.sadakat.qit.core.data.audio

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import dev.sadakat.qit.core.domain.audio.DownloadAggregation
import dev.sadakat.qit.core.domain.audio.FileDownloadState
import dev.sadakat.qit.core.domain.audio.QuranAudioUrls
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [FileDownloadState] of a Media3 [Download], or null when its row is on its way out: REMOVING
 * rows are deleted by the manager and must not count towards any surah.
 */
@OptIn(UnstableApi::class)
internal fun fileDownloadStateOf(download: Download): FileDownloadState? =
    when (download.state) {
        Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_RESTARTING, Download.STATE_STOPPED ->
            FileDownloadState.ACTIVE
        Download.STATE_COMPLETED -> FileDownloadState.COMPLETED
        Download.STATE_FAILED -> FileDownloadState.FAILED
        else -> null
    }

/** [SurahDownloads] backed by [QuranCache.downloadManager] and [QuranDownloadService]. */
@OptIn(UnstableApi::class)
class MediaSurahDownloads(
    private val context: Context,
    private val quranCache: QuranCache
) : SurahDownloads {

    private val downloadManager = quranCache.downloadManager

    /** DownloadManager lives on the main thread; blocks are posted there when called off-main. */
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lock = Any()
    private val fileStates = HashMap<String, FileDownloadState>()

    /** Ids a listener event has touched; such events win over the initial index load. */
    private val eventIds = HashSet<String>()

    private val _states = MutableStateFlow<Map<Int, Map<Track, SurahDownloadState>>>(emptyMap())
    override val states: StateFlow<Map<Int, Map<Track, SurahDownloadState>>> = _states.asStateFlow()

    private val listener = object : DownloadManager.Listener {
        override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) {
            val id = download.request.id
            val state = fileDownloadStateOf(download)
            synchronized(lock) {
                eventIds.add(id)
                if (state == null) fileStates.remove(id) else fileStates[id] = state
                recomputeLocked(DownloadAggregation.pairsContaining(id))
            }
        }

        override fun onDownloadRemoved(manager: DownloadManager, download: Download) {
            val id = download.request.id
            synchronized(lock) {
                eventIds.add(id)
                fileStates.remove(id)
                recomputeLocked(DownloadAggregation.pairsContaining(id))
            }
        }
    }

    init {
        // Register before the initial read so no change is missed; recomputeLocked(ALL_PAIRS)
        // afterwards folds in whatever the index held all along.
        onMain { downloadManager.addListener(listener) }
        scope.launch {
            loadIndex()
            synchronized(lock) { recomputeLocked(ALL_PAIRS) }
        }
    }

    override fun download(surah: Int, tracks: List<Track>) {
        val requests = tracks
            .flatMap { track -> QuranAudioUrls.surahFiles(surah, track) }
            .map { file -> DownloadRequest.Builder(file.id, Uri.parse(file.url)).build() }
        onMain {
            for (request in requests) {
                // The index is authoritative even before our initial load has finished.
                if (downloadManager.downloadIndex.getDownload(request.id)?.state == Download.STATE_COMPLETED) continue
                downloadManager.addDownload(request)
            }
            startService()
        }
    }

    override fun remove(surah: Int, tracks: List<Track>) {
        val removing = tracks.mapTo(hashSetOf()) { surah to it }
        val tracked = _states.value
        val doomed = tracks
            .flatMap { track -> QuranAudioUrls.surahFiles(surah, track) }
            .map { file -> file.id }
            .filterNot { id ->
                // The shared basmala ("ar/1"/"en/1") outlives a surah while any other tracked
                // pair still needs it. Per-surah files ("bn/intro/…", verses) always go.
                val sharers = DownloadAggregation.pairsContaining(id)
                sharers.size > 1 && sharers.any { pair ->
                    pair !in removing && tracked[pair.first]?.containsKey(pair.second) == true
                }
            }
        onMain { for (id in doomed) downloadManager.removeDownload(id) }
    }

    /** Reads the whole index once; the cursor touches the database, so this stays off the main thread. */
    private fun loadIndex() {
        downloadManager.downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) {
                val download = cursor.download
                val state = fileDownloadStateOf(download) ?: continue
                synchronized(lock) {
                    if (download.request.id !in eventIds) fileStates[download.request.id] = state
                }
            }
        }
    }

    /**
     * Recomputes the pairs' states from the current file states. Callers hold [lock]: snapshotting
     * and publishing under the same lock as the events guarantees whichever runs last sees the
     * freshest files — a late initial load can never erase what an event already published.
     */
    private fun recomputeLocked(pairs: Collection<Pair<Int, Track>>) {
        if (pairs.isEmpty()) return
        val snapshot = fileStates.toMap()
        _states.update { current ->
            val next = HashMap(current)
            for ((surah, track) in pairs) {
                val state = DownloadAggregation.stateOf(surah, track, snapshot)
                val inner = next[surah]?.toMutableMap() ?: mutableMapOf()
                if (state == null) inner.remove(track) else inner[track] = state
                if (inner.isEmpty()) next.remove(surah) else next[surah] = inner
            }
            next
        }
    }

    /**
     * Starts the foreground service so downloads survive the app going away. Android 12+ forbids
     * that from the background — exactly where the watch's message listener runs — so fall back to
     * a plain start and finally give up: the manager keeps downloading in-process either way.
     */
    private fun startService() {
        try {
            DownloadService.startForeground(context, QuranDownloadService::class.java)
        } catch (e: Exception) {
            try {
                DownloadService.start(context, QuranDownloadService::class.java)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start QuranDownloadService; downloading in-process", e)
            }
        }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    companion object {
        private const val TAG = "MediaSurahDownloads"

        private val ALL_PAIRS = buildList {
            for (surah in 1..QuranMeta.SURAH_COUNT) {
                for (track in Track.entries) add(surah to track)
            }
        }
    }
}
