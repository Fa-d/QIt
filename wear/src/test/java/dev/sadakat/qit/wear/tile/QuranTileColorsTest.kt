package dev.sadakat.qit.wear.tile

import androidx.compose.ui.graphics.toArgb
import dev.sadakat.qit.wear.presentation.theme.QItWearColorScheme
import org.junit.Assert.assertEquals
import org.junit.Test

/** The tile wears exactly the watch app's colors, not the tile library's defaults. */
class QuranTileColorsTest {

    @Test
    fun `every role matches the watch theme`() {
        val app = QItWearColorScheme
        val tile = QuranTileColorScheme
        val pairs = listOf(
            app.primary to tile.primary, app.primaryDim to tile.primaryDim,
            app.primaryContainer to tile.primaryContainer,
            app.onPrimary to tile.onPrimary, app.onPrimaryContainer to tile.onPrimaryContainer,
            app.secondary to tile.secondary, app.secondaryDim to tile.secondaryDim,
            app.secondaryContainer to tile.secondaryContainer, app.onSecondary to tile.onSecondary,
            app.onSecondaryContainer to tile.onSecondaryContainer,
            app.tertiary to tile.tertiary, app.tertiaryDim to tile.tertiaryDim,
            app.tertiaryContainer to tile.tertiaryContainer,
            app.onTertiary to tile.onTertiary, app.onTertiaryContainer to tile.onTertiaryContainer,
            app.surfaceContainerLow to tile.surfaceContainerLow, app.surfaceContainer to tile.surfaceContainer,
            app.surfaceContainerHigh to tile.surfaceContainerHigh, app.onSurface to tile.onSurface,
            app.onSurfaceVariant to tile.onSurfaceVariant, app.outline to tile.outline,
            app.outlineVariant to tile.outlineVariant,
            app.background to tile.background, app.onBackground to tile.onBackground,
            app.error to tile.error, app.errorDim to tile.errorDim, app.errorContainer to tile.errorContainer,
            app.onError to tile.onError, app.onErrorContainer to tile.onErrorContainer,
        )
        pairs.forEach { (compose, layout) -> assertEquals(compose.toArgb(), layout.staticArgb) }
    }
}
