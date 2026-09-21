package dev.sadakat.qit.core.designsystem.scale

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Motion tokens. Every animation in the apps goes through these, so the feel of the app is tuned
 * here (and can later map onto Material 3 Expressive's MotionScheme in one place).
 */
@Immutable
data class QItMotion(
    val durationShort: Int = 150,
    val durationMedium: Int = 300,
    val durationLong: Int = 500,
    /** Most transitions: quick start, gentle settle. */
    val easingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    /** Things arriving on screen. */
    val easingEnter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
    /** Things leaving the screen. */
    val easingExit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f),
) {
    /** Color, alpha and size changes of something that stays on screen. */
    fun <T> standard(): FiniteAnimationSpec<T> = tween(durationMedium, easing = easingStandard)

    fun <T> enter(): FiniteAnimationSpec<T> = tween(durationMedium, easing = easingEnter)

    fun <T> exit(): FiniteAnimationSpec<T> = tween(durationShort, easing = easingExit)

    /** Movement of things the user touches (sheets, the play button): springy, no overshoot drama. */
    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = SPATIAL_DAMPING, stiffness = Spring.StiffnessMediumLow)

    private companion object {
        const val SPATIAL_DAMPING = 0.8f
    }
}
