package dev.sadakat.qandeel.wear.presentation

/** Intent extras [MainActivity] understands; the tile launches the app with them. */
object WearIntents {
    /** Boolean extra: open straight on Now playing (the tile's Continue / Now playing action). */
    const val EXTRA_OPEN_NOW_PLAYING = "dev.sadakat.qandeel.wear.extra.OPEN_NOW_PLAYING"

    /** Boolean extra: also resume listening (the tile's Continue / Resume action). */
    const val EXTRA_RESUME_PLAYBACK = "dev.sadakat.qandeel.wear.extra.RESUME_PLAYBACK"
}
