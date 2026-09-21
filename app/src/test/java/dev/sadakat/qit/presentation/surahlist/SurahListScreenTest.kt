package dev.sadakat.qit.presentation.surahlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.testing.TestQuran
import dev.sadakat.qit.ui.theme.QItTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SurahListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val surahs = listOf(TestQuran.surah(1), TestQuran.surah(2), TestQuran.surah(112))

    @Test
    fun `shows the surah rows`() {
        composeRule.setContent {
            QItTheme {
                SurahListScreen(
                    state = SurahListUiState(surahs = surahs),
                    onSearchQueryChange = {},
                    onSurahClick = {},
                    onContinueListening = {},
                )
            }
        }
        composeRule.onNodeWithText("Al-Faatiha").assertIsDisplayed()
        composeRule.onNodeWithText("Al-Baqara").assertIsDisplayed()
        composeRule.onNodeWithText("Al-Ikhlaas").assertIsDisplayed()
    }

    @Test
    fun `typing in the search field reports the query and the rows follow the state`() {
        var query = ""
        composeRule.setContent {
            QItTheme {
                // Hoisted query, like the route does through the ViewModel.
                var shownQuery by remember { mutableStateOf("") }
                SurahListScreen(
                    state = SurahListUiState(
                        query = shownQuery,
                        surahs = surahs.filter { it.nameEnglish.contains(shownQuery, ignoreCase = true) },
                    ),
                    onSearchQueryChange = {
                        query = it
                        shownQuery = it
                    },
                    onSurahClick = {},
                    onContinueListening = {},
                )
            }
        }
        composeRule.onNodeWithTag("surah_search").performTextInput("baqara")
        assertEquals("baqara", query)
        composeRule.onNodeWithText("Al-Baqara").assertIsDisplayed()
        composeRule.onNodeWithText("Al-Faatiha").assertDoesNotExist()
    }

    @Test
    fun `tapping a row opens that surah`() {
        var opened = 0
        composeRule.setContent {
            QItTheme {
                SurahListScreen(
                    state = SurahListUiState(surahs = surahs),
                    onSearchQueryChange = {},
                    onSurahClick = { opened = it },
                    onContinueListening = {},
                )
            }
        }
        composeRule.onNodeWithText("Al-Baqara").performClick()
        assertEquals(2, opened)
    }

    @Test
    fun `tapping the continue listening card resumes that ayah`() {
        var resumed: Pair<Int, Int>? = null
        composeRule.setContent {
            QItTheme {
                SurahListScreen(
                    state = SurahListUiState(
                        surahs = surahs,
                        continueListening = ContinueListening(2, "Al-Baqara", 255),
                    ),
                    onSearchQueryChange = {},
                    onSurahClick = {},
                    onContinueListening = { resumed = it.surah to it.ayah },
                )
            }
        }
        composeRule.onNodeWithText("Al-Baqara 2:255").assertIsDisplayed()
        composeRule.onNodeWithTag("continue_listening").performClick()
        assertEquals(2 to 255, resumed)
    }

    @Test
    fun `shows the error state when the text failed to load`() {
        composeRule.setContent {
            QItTheme {
                SurahListScreen(
                    state = SurahListUiState(loadFailed = true),
                    onSearchQueryChange = {},
                    onSurahClick = {},
                    onContinueListening = {},
                )
            }
        }
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(dev.sadakat.qit.R.string.load_error)).assertIsDisplayed()
        composeRule.onNodeWithText("Al-Baqara").assertDoesNotExist()
    }
}
