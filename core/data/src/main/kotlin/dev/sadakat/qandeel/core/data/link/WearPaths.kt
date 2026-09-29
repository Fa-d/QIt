package dev.sadakat.qandeel.core.data.link

object WearPaths {
    /** Phone → watch message: download a surah on the watch. Payload: QuranDownloadMessage JSON. */
    const val QURAN_DOWNLOAD = "/quran/download"

    /**
     * Watch → phone data item: what the watch heard ([ListeningSnapshotMessage] JSON in the
     * [LISTENING_ASSET] asset). One item per watch, kept up to date; the phone adds it to its own.
     */
    const val LISTENING = "/quran/listening"

    /** Phone → watch data item: the listening history was reset ([ListeningResetMessage] JSON in [LISTENING_ASSET]). */
    const val LISTENING_RESET = "/quran/listening/reset"

    /**
     * Phone → watch data item: the Bangla voice to recite with (its code in [BANGLA_VOICE_KEY]).
     * A data item, not a message, so a watch that was away gets the voice when it reconnects.
     */
    const val BANGLA_VOICE = "/quran/bangla-voice"

    /** The asset key holding a listening item's payload (assets have no size limit, unlike data maps). */
    const val LISTENING_ASSET = "payload"

    /** The data map key holding the Bangla voice item's payload (a voice code is tiny, unlike a snapshot). */
    const val BANGLA_VOICE_KEY = "voice"

    // Capability names (declared in each app's res/values/wear.xml)
    const val CAPABILITY_PHONE_APP = "qandeel_phone_app"
    const val CAPABILITY_WATCH_APP = "qandeel_watch_app"
}
