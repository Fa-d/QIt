package dev.sadakat.qandeel.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sadakat.qandeel.R
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.player.PlaybackSpeed
import dev.sadakat.qandeel.core.domain.player.RepeatSetting
import dev.sadakat.qandeel.core.domain.player.SleepTimerStatus
import java.util.Locale

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

/** A time as a clock reads it: "3:07", or "1:02:45" from an hour on. */
fun clockText(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / MS_PER_SECOND
    val hours = seconds / SECONDS_PER_HOUR
    val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val rest = seconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, rest)
    }
}

/** Full mode names for the mode menu ("Arabic + English"). */
@Composable
fun modeName(mode: RecitationMode): String = stringResource(
    when (mode) {
        RecitationMode.ARABIC_ONLY -> R.string.player_mode_full_arabic
        RecitationMode.ARABIC_ENGLISH -> R.string.player_mode_full_english
        RecitationMode.ARABIC_BANGLA -> R.string.player_mode_full_bangla
    },
)

/** The voice's narrator, as the mode menu and the settings sheet name them. */
@Composable
fun banglaVoiceName(voice: BanglaVoice): String = stringResource(
    when (voice) {
        BanglaVoice.ISLAMIC_FOUNDATION -> R.string.bangla_voice_islamic_foundation
        BanglaVoice.SAYED_ISMAT_TOHA -> R.string.bangla_voice_toha
        BanglaVoice.SHAREEF_BAEZEED_MAHMOOD -> R.string.bangla_voice_baezeed
    },
)

private const val MS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
