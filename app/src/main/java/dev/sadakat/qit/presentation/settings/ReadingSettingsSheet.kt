package dev.sadakat.qit.presentation.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The reading-comfort sheet (Arabic size, translation, follow-along, theme), opened from the home
 * screen and the reader. Contract for the screens that host it; the settings slice fills it in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {}
}
