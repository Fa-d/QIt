package dev.sadakat.qit.core.designsystem.component

import androidx.compose.ui.unit.dp

/** Watch layout values that aren't steps of a shared scale. */
object WearTokens {
    /** The ayah-progress ring around the now-playing controls. */
    val ProgressRingStroke = 4.dp

    /** Crown travel that counts as one volume step on Now playing. */
    const val VOLUME_CROWN_PIXELS_PER_STEP = 24f
}
