package dev.sadakat.qandeel.core.ui.kit

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/** A bottom sheet's state: expanded and hidden, and, unless skipped, half open. */
@Stable
class QandeelSheetState
@OptIn(ExperimentalMaterial3Api::class)
internal constructor(internal val state: SheetState) {
    /** Animates the sheet away; call before removing it from composition to keep the exit smooth. */
    @OptIn(ExperimentalMaterial3Api::class)
    suspend fun hide() = state.hide()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberQandeelSheetState(skipPartiallyExpanded: Boolean = false): QandeelSheetState {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    return remember(state) { QandeelSheetState(state) }
}
