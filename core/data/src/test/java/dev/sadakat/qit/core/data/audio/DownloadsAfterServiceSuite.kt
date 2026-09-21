package dev.sadakat.qit.core.data.audio

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Pins the execution order that leaked state between these classes: creating the service builds
 * the process-wide [QuranCache], which Robolectric then carried into the downloads tests with a
 * dead RequirementsWatcher receiver. Gradle's own class order is an implementation detail, so the
 * order is pinned here instead.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(QuranDownloadServiceTest::class, MediaSurahDownloadsTest::class)
class DownloadsAfterServiceSuite
