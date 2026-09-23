package dev.sadakat.qit.presentation.player

import androidx.annotation.StringRes
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.player.PlaybackError

/** What to tell the user about [error]. */
@StringRes
fun PlaybackError.messageRes(): Int = when (this) {
    PlaybackError.NETWORK -> R.string.playback_error_network
    PlaybackError.FAILED -> R.string.playback_error_failed
}
