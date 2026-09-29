package dev.sadakat.qandeel.wear.presentation.mode

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.AppScaffold
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ModeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows one row per mode with the current one selected`() {
        composeRule.setContent {
            AppScaffold { ModeScreen(uiState = WearModeUiState(RecitationMode.ARABIC_BANGLA), onSelect = {}) }
        }

        composeRule.onNodeWithText("Recitation").assertExists()
        composeRule.onNodeWithText("Arabic").assertExists()
        composeRule.onNodeWithText("Arabic + English").assertExists()
        composeRule.onNodeWithText("Arabic + Bangla").assertExists()

        val selected = composeRule.onNodeWithText("Arabic + Bangla").fetchSemanticsNode().config
        assertEquals(true, selected[SemanticsProperties.Selected])
    }

    @Test
    fun `selecting a mode reports it back`() {
        val picked = mutableListOf<RecitationMode>()
        composeRule.setContent {
            AppScaffold {
                ModeScreen(uiState = WearModeUiState(RecitationMode.ARABIC_ONLY), onSelect = { picked += it })
            }
        }

        composeRule.onNodeWithText("Arabic + Bangla").performClick()
        assertEquals(listOf(RecitationMode.ARABIC_BANGLA), picked)
    }
}
