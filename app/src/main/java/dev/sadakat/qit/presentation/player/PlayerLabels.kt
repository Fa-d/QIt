package dev.sadakat.qit.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepTimerStatus

/** Short mode names for the segmented selector ("Arabic", "+ English", "+ Bangla"). */
@Composable
fun modeLabel(mode: RecitationMode): String = stringResource(
    when (mode) {
        RecitationMode.ARABIC_ONLY -> R.string.player_mode_arabic
        RecitationMode.ARABIC_ENGLISH -> R.string.player_mode_english
        RecitationMode.ARABIC_BANGLA -> R.string.player_mode_bangla
    },
)

/** "Repeat", "Ayah ×3", "Ayah ∞", "3–7 ×2", "3–7 ∞". */
@Composable
fun repeatLabel(repeat: RepeatSetting): String = when (repeat) {
    RepeatSetting.Off -> stringResource(R.string.player_repeat_off)

    is RepeatSetting.Ayah -> repeat.times?.let { stringResource(R.string.player_repeat_ayah, it) }
        ?: stringResource(R.string.player_repeat_ayah_forever)

    is RepeatSetting.Range -> repeat.times?.let {
        stringResource(R.string.player_repeat_range, repeat.from, repeat.to, it)
    }
        ?: stringResource(R.string.player_repeat_range_forever, repeat.from, repeat.to)
}

/** "0.75", "1", "1.25", "1.5" — the factor without a trailing ".0". */
fun speedFactor(speed: PlaybackSpeed): String = speed.factor.toString().removeSuffix(".0")

/** "12:34" while counting, "End of surah", or null when no timer runs. */
@Composable
fun sleepLabel(status: SleepTimerStatus): String? = when (status) {
    SleepTimerStatus.Off -> null
    SleepTimerStatus.EndOfSurah -> stringResource(R.string.player_sleep_end_of_surah)
    is SleepTimerStatus.Counting -> remaining(status.remainingMs)
    is SleepTimerStatus.FadingOut -> remaining(status.remainingMs)
}

@Composable
private fun remaining(ms: Long): String {
    val seconds = (ms + MS_PER_SECOND - 1) / MS_PER_SECOND
    return stringResource(
        R.string.player_sleep_remaining,
        (seconds / SECONDS_PER_MINUTE).toInt(),
        (
            seconds %
                SECONDS_PER_MINUTE
            ).toInt(),
    )
}

private const val MS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
