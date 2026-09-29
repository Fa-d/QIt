package dev.sadakat.qandeel.wear.presentation.juz

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.AppScaffold
import dev.sadakat.qandeel.core.domain.model.AyahRef
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class JuzScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val uiState = WearJuzUiState(
        rows = listOf(
            JuzRowUiModel(juz = 1, start = AyahRef(1, 1), surahName = "Al-Faatiha"),
            JuzRowUiModel(juz = 15, start = AyahRef(17, 1), surahName = null),
        ),
    )

    @Test
    fun `rows show the juz and where it starts`() {
        composeRule.setContent {
            AppScaffold { JuzScreen(uiState = uiState, onJuzClick = {}) }
        }

        composeRule.onNodeWithText("Juz 1").assertExists()
        composeRule.onNodeWithText("Al-Faatiha 1:1").assertExists()
        composeRule.onNodeWithText("Juz 15").assertExists()
        // Unknown names (text source failed) fall back to "Surah N".
        composeRule.onNodeWithText("Surah 17 17:1").assertExists()
    }

    @Test
    fun `a tap reports the juz start to open the surah at`() {
        val starts = mutableListOf<AyahRef>()
        composeRule.setContent {
            AppScaffold { JuzScreen(uiState = uiState, onJuzClick = { starts += it }) }
        }

        composeRule.onNodeWithText("Juz 15").performClick()
        assertEquals(listOf(AyahRef(17, 1)), starts)
    }
}
