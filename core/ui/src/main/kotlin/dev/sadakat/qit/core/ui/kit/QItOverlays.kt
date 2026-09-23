package dev.sadakat.qit.core.ui.kit

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.ui.kit.glass.LocalQItSurfaceMode
import dev.sadakat.qit.core.ui.kit.glass.QItGlassLook
import dev.sadakat.qit.core.ui.kit.glass.QItSurfaceMode
import dev.sadakat.qit.core.ui.kit.glass.QItSurfaceRole
import dev.sadakat.qit.core.ui.kit.glass.glassLook

/**
 * A modal bottom sheet in the current look. In glass it is a frosted pane, and the app behind it is
 * blurred where the system allows (Android 12+, blur not turned off).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QItSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    state: QItSheetState = rememberQItSheetState(),
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = glassLook(containerColor, QItSurfaceRole.SHEET)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = state.state,
        containerColor = glass?.tint ?: containerColor,
    ) {
        if (glass != null) BlurBehindWindow()
        content()
    }
}

/** An alert dialog in the current look: frosted in glass, over the app blurred where allowed. */
@Composable
fun QItAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    val glass = glassLook(AlertDialogDefaults.containerColor, QItSurfaceRole.SHEET)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = if (glass != null && text != null) {
            {
                BlurBehindWindow()
                text()
            }
        } else {
            text
        },
        containerColor = glass?.tint ?: AlertDialogDefaults.containerColor,
    )
}

/** A dropdown menu in the current look: a frosted pane with a light edge in glass. */
@Composable
fun QItMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = glassLook(MenuDefaults.containerColor, QItSurfaceRole.SHEET)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = glass?.tint ?: MenuDefaults.containerColor,
        shadowElevation = if (glass == null) MenuDefaults.ShadowElevation else 0.dp,
        border = glass?.edgeStroke(),
        content = content,
    )
}

private fun QItGlassLook.edgeStroke(): BorderStroke? = BorderStroke(hairline, hairlineColor).takeIf { hairline > 0.dp }

/**
 * Blurs whatever is behind the window this is composed in (a sheet's or a dialog's), when frosted
 * glass is on. The system skips it when cross-window blur is off (battery saver, `wm disable-blur`),
 * and the pane's own tint keeps it readable then.
 */
@Composable
private fun BlurBehindWindow(radius: Dp = QItTheme.surfaces.blurRadius) {
    if (LocalQItSurfaceMode.current != QItSurfaceMode.FROSTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val window = LocalView.current.dialogWindow() ?: return
    val px = with(LocalDensity.current) { radius.roundToPx() }
    DisposableEffect(window, px) {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply { blurBehindRadius = px }
        onDispose { }
    }
}

/** The dialog window hosting this view: a sheet's layout is the provider, a dialog's is its parent. */
private fun View.dialogWindow(): Window? =
    (this as? DialogWindowProvider)?.window ?: (parent as? DialogWindowProvider)?.window
