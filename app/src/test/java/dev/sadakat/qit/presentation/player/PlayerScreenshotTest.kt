package dev.sadakat.qit.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.testing.snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class PlayerScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun miniLight() = composeRule.snapshot("player_mini_light") { Mini() }

    @Test
    fun miniDark() = composeRule.snapshot("player_mini_dark", darkTheme = true) { Mini() }

    @Test
    fun fullLight() = composeRule.snapshot("player_full_light") { Full(state()) }

    @Test
    fun fullDark() = composeRule.snapshot("player_full_dark", darkTheme = true) { Full(state()) }

    @Test
    @Config(fontScale = 1.3f)
    fun fullLargeText() = composeRule.snapshot("player_full_stress", arabicScale = 1.75f) {
        val busy =
            state(
                repeat = RepeatSetting.Range(3, 7, 2),
                speed = PlaybackSpeed.X0_75,
                sleep = SleepTimerStatus.EndOfSurah,
            )
        Full(busy)
    }

    @Composable
    private fun Mini() {
        MiniPlayer(state = state(), onExpand = {}, onTogglePlayPause = {}, onNext = {})
    }

    @Composable
    private fun Full(state: PlayerUiState) {
        NowPlayingContent(state = state, actions = NO_ACTIONS)
    }

    private fun state(
        repeat: RepeatSetting = RepeatSetting.Ayah(3),
        speed: PlaybackSpeed = PlaybackSpeed.X1,
        sleep: SleepTimerStatus = SleepTimerStatus.Counting(754_000L),
    ) = PlayerUiState(
        nowPlaying = NowPlaying(
            surah = 18,
            ayah = 10,
            track = Track.ARABIC,
            mode = RecitationMode.ARABIC_ENGLISH,
            isPlaying = true,
            isBuffering = false,
            speed = speed,
            repeat = repeat,
        ),
        surahName = "Al-Kahf",
        ayahArabic = "إِذْ أَوَى ٱلْفِتْيَةُ إِلَى ٱلْكَهْفِ فَقَالُوا۟ رَبَّنَآ ءَاتِنَا مِن لَّدُنكَ رَحْمَةً " +
            "وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا",
        ayahTranslation = "[Mention] when the youths retreated to the cave and said, \"Our Lord, grant us from " +
            "Yourself mercy and prepare for us from our affair right guidance.\"",
        sleepTimer = sleep,
    )

    private companion object {
        val NO_ACTIONS = NowPlayingActions({}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {})
    }
}
