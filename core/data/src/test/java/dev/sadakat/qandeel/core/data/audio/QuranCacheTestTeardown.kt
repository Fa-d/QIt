package dev.sadakat.qandeel.core.data.audio

import androidx.media3.exoplayer.offline.DownloadService

/**
 * Tears down what [QuranCache.get] leaves behind process-wide so the next test class starts clean.
 *
 * Robolectric reuses one sandbox — one set of statics, hence one [QuranCache] singleton — across
 * test classes while giving each class a fresh Application. Both must go while the *current*
 * class's Application is still alive: the manager's RequirementsWatcher receiver can only be
 * unregistered from the environment it was registered in, and [DownloadService]'s static helpers
 * would otherwise keep listening to the released manager.
 */
internal fun releaseProcessWideQuranCache() {
    DownloadService.clearDownloadManagerHelpers()
    QuranCache.resetForTests()
}
