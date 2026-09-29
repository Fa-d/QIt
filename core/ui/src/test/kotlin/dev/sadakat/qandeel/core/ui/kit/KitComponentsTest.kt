package dev.sadakat.qandeel.core.ui.kit

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkins
import dev.sadakat.qandeel.core.designsystem.skin.QandeelStyle
import dev.sadakat.qandeel.core.designsystem.skin.QandeelTone
import dev.sadakat.qandeel.core.ui.kit.glass.LocalQandeelSurfaceMode
import dev.sadakat.qandeel.core.ui.kit.glass.QandeelSurfaceMode
import dev.sadakat.qandeel.core.ui.theme.QandeelMaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KitComponentsTest {

    @get:Rule
    val rule = createComposeRule()

    private fun setContent(
        style: QandeelStyle = QandeelStyle.GLASS,
        mode: QandeelSurfaceMode? = QandeelSurfaceMode.FROSTED,
        content: @Composable () -> Unit,
    ) {
        rule.setContent {
            QandeelMaterialTheme(QandeelSkins.of(style, QandeelTone.LIGHT), surfaceMode = mode, content = content)
        }
    }

    @Test
    fun `labels that fit make a segmented row`() {
        var picked = ""
        setContent {
            QandeelSegmentedToggle(listOf("One", "Two"), selected = "One", onSelect = { picked = it }, label = { it })
        }
        rule.onNodeWithText("Two").performClick()

        assertEquals("Two", picked)
        rule.onAllNodesWithText("One").assertCountEquals(1)
    }

    @Test
    fun `labels that don't fit stack as radio buttons`() {
        var picked = ""
        setContent {
            QandeelSegmentedToggle(
                options = listOf("A rather long first choice", "An even longer second choice"),
                selected = "A rather long first choice",
                onSelect = { picked = it },
                label = { it },
                modifier = Modifier.width(240.dp),
            )
        }
        rule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.RadioButton,
            ).and(hasText("An even longer second choice")),
        )
            .performClick()

        assertEquals("An even longer second choice", picked)
    }

    @Test
    fun `a switch row toggles from anywhere on the row`() {
        setContent {
            var on by remember { mutableStateOf(false) }
            QandeelSwitchRow(title = "Follow along", checked = on, onCheckedChange = {
                on = it
            }, supporting = "Keep it in view")
        }
        rule.onNodeWithText("Follow along").assertIsOff().performClick()
        rule.onNodeWithText("Follow along").assertIsOn()
    }

    @Test
    fun `sheet, dialog and menu show their content in glass`() {
        setContent {
            QandeelSheet(onDismissRequest = {}) { Text("In the sheet") }
            QandeelAlertDialog(
                onDismissRequest = {},
                confirmButton = { Text("OK") },
                title = { Text("Remove?") },
                text = { Text("It goes away") },
            )
            QandeelMenu(expanded = true, onDismissRequest = {}) { Text("In the menu") }
        }
        rule.onNodeWithText("In the sheet").assertExists()
        rule.onNodeWithText("It goes away").assertExists()
        rule.onNodeWithText("In the menu").assertExists()
    }

    @Test
    fun `solid looks draw the same components solid`() {
        setContent(style = QandeelStyle.MUSHAF, mode = null) {
            QandeelSheet(onDismissRequest = {}) { Text("In the sheet") }
            QandeelFloatingChip(onClick = {}, label = { Text("Chip") })
            QandeelSearchField(query = "36", onQueryChange = {}, placeholder = "Search", clearLabel = "Clear")
            QandeelCard(onClick = {}, emphasis = QandeelEmphasis.TERTIARY) { Text("Card") }
        }
        rule.onNodeWithText("In the sheet").assertExists()
        rule.onNodeWithText("Chip").assertExists()
        rule.onNodeWithText("Card").assertExists()
    }

    @Test
    fun `glass follows the device when its mode isn't pinned`() {
        var mode = QandeelSurfaceMode.OPAQUE
        setContent(mode = null) { mode = LocalQandeelSurfaceMode.current }
        rule.waitForIdle()

        // Robolectric's device: API 36, blur supported, no battery saver.
        assertNotEquals(QandeelSurfaceMode.OPAQUE, mode)
    }
}
