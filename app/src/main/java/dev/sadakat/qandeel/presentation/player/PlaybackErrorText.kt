package dev.sadakat.qandeel.presentation.player

import androidx.annotation.StringRes
import dev.sadakat.qandeel.R
import dev.sadakat.qandeel.core.domain.player.PlaybackError

/** What to tell the user about [error]. */
@StringRes
fun PlaybackError.messageRes(): Int = when (this) {
    PlaybackError.NETWORK -> R.string.playback_error_network
    PlaybackError.FAILED -> R.string.playback_error_failed
}
