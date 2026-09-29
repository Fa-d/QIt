// The surface mode is a theme value, provided once by the theme like the design tokens.
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package dev.sadakat.qandeel.core.ui.kit.glass

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.sadakat.qandeel.core.designsystem.scale.QandeelSurfaces

/** How translucent surfaces are drawn on this device right now. */
enum class QandeelSurfaceMode {
    /** Translucent over a blurred backdrop. */
    FROSTED,

    /** Translucent over the unblurred page, a little more opaque to stay readable (blur unavailable). */
    TINTED,

    /** Solid: the look is opaque, or the user asked for less (battery saver, more contrast). */
    OPAQUE,
}

/** The current [QandeelSurfaceMode]; provided by the theme. */
val LocalQandeelSurfaceMode = staticCompositionLocalOf { QandeelSurfaceMode.OPAQUE }

/** What the device says about blur and the user's comfort settings. */
data class SurfaceSignals(
    val sdkInt: Int,
    /** Battery saver: skip the GPU cost of blurring. */
    val powerSave: Boolean = false,
    /** Android's high-contrast text (readable on API 36+). */
    val highContrastText: Boolean = false,
    /** Android's contrast setting (API 34+): 0 is standard, above 0 asks for more. */
    val contrastLevel: Float = 0f,
)

/**
 * The mode for [surfaces] given [signals]. Legibility wins over looks: anything that asks for less
 * visual noise or more contrast gets solid surfaces, and no blur support gets a stronger tint.
 */
fun resolveSurfaceMode(surfaces: QandeelSurfaces, signals: SurfaceSignals): QandeelSurfaceMode = when {
    !surfaces.isTranslucent -> QandeelSurfaceMode.OPAQUE

    signals.powerSave || signals.highContrastText || signals.contrastLevel > 0f -> QandeelSurfaceMode.OPAQUE

    // Blur needs RenderEffect (Android 12), and Android 12.0 itself invalidates blurred layers unreliably.
    signals.sdkInt <= Build.VERSION_CODES.S || surfaces.blurRadius <= 0.dp -> QandeelSurfaceMode.TINTED

    else -> QandeelSurfaceMode.FROSTED
}

/** The mode for [surfaces] on this device, updated live as the user changes the settings it reads. */
@Composable
fun rememberSurfaceMode(surfaces: QandeelSurfaces): QandeelSurfaceMode {
    // Opaque looks never read the device: no receivers or listeners for them.
    if (!surfaces.isTranslucent) return QandeelSurfaceMode.OPAQUE
    return resolveSurfaceMode(surfaces, rememberSurfaceSignals())
}

/** The device's [SurfaceSignals], kept current while in composition. */
@Composable
fun rememberSurfaceSignals(): SurfaceSignals {
    val context = LocalContext.current.applicationContext
    var signals by remember(context) { mutableStateOf(readSignals(context)) }
    DisposableEffect(context) {
        val refresh = { signals = readSignals(context) }
        val unregister = listOfNotNull(
            watchPowerSave(context, refresh),
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
                watchContrast(context, refresh)
            } else {
                null
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) watchHighContrastText(context, refresh) else null,
        )
        onDispose { unregister.forEach { it() } }
    }
    return signals
}

private fun readSignals(context: Context): SurfaceSignals {
    val power = context.getSystemService(PowerManager::class.java)
    return SurfaceSignals(
        sdkInt = Build.VERSION.SDK_INT,
        powerSave = power?.isPowerSaveMode == true,
        highContrastText = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && highContrastText(context),
        contrastLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) contrast(context) else 0f,
    )
}

/** Listens for battery saver switching on or off; returns the unregistering call. */
private fun watchPowerSave(context: Context, onChange: () -> Unit): () -> Unit {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onChange()
    }
    val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
    // A system broadcast: delivered to unexported receivers too.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        context.registerReceiver(receiver, filter)
    }
    return { context.unregisterReceiver(receiver) }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun contrast(context: Context): Float = context.getSystemService(UiModeManager::class.java)?.contrast ?: 0f

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun watchContrast(context: Context, onChange: () -> Unit): (() -> Unit)? {
    val manager = context.getSystemService(UiModeManager::class.java) ?: return null
    val listener = UiModeManager.ContrastChangeListener { onChange() }
    manager.addContrastChangeListener(context.mainExecutor, listener)
    return { manager.removeContrastChangeListener(listener) }
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
private fun highContrastText(context: Context): Boolean =
    context.getSystemService(AccessibilityManager::class.java)?.isHighContrastTextEnabled == true

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
private fun watchHighContrastText(context: Context, onChange: () -> Unit): (() -> Unit)? {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return null
    val listener = AccessibilityManager.HighContrastTextStateChangeListener { onChange() }
    manager.addHighContrastTextStateChangeListener(context.mainExecutor, listener)
    return { manager.removeHighContrastTextStateChangeListener(listener) }
}
