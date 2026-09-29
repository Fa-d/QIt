package dev.sadakat.qit.presentation.about

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sadakat.qit.R
import dev.sadakat.qit.ui.theme.QItAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var back = false

    private fun setContent() {
        composeRule.setContent {
            QItAppTheme { AboutScreen(versionName = "1.2.3", onBack = { back = true }) }
        }
    }

    @Test
    fun `shows the version`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.about_version, "1.2.3")).assertIsDisplayed()
    }

    @Test
    fun `credits the CC BY word timings and states the privacy note`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.credit_quran_align_detail)).performScrollTo()
        composeRule.onNodeWithText(context.getString(R.string.about_privacy)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `each section title is a heading`() {
        setContent()
        val headings = composeRule.onAllNodes(isHeading()).fetchSemanticsNodes().size
        // Recitation, text, word by word, font and software, privacy.
        assertEquals(5, headings)
    }

    @Test
    fun `back leaves the screen`() {
        setContent()
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_back)).performClick()
        assertTrue(back)
    }
}
