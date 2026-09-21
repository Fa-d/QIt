package dev.sadakat.qit.core.data.link

object WearPaths {
    /** Phone → watch message: download a surah on the watch. Payload: QuranDownloadMessage JSON. */
    const val QURAN_DOWNLOAD = "/quran/download"

    // Capability names (declared in each app's res/values/wear.xml)
    const val CAPABILITY_PHONE_APP = "qit_phone_app"
    const val CAPABILITY_WATCH_APP = "qit_watch_app"
}
