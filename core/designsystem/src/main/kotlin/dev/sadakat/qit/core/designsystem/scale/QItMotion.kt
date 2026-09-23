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
 * here (and can later map onto Material 3 Expressive's MotionScheme in one place). With [springs]
 * set, every spec is a spring, the way Material 3 Expressive moves; otherwise color and fade changes
 * use timed curves.
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
    /** Springs for every spec instead of timed curves; null keeps the curves. */
    val springs: QItSprings? = null,
) {
    /** Color, alpha and size changes of something that stays on screen. */
    fun <T> standard(): FiniteAnimationSpec<T> = springs?.effects() ?: tween(durationMedium, easing = easingStandard)

    fun <T> enter(): FiniteAnimationSpec<T> = springs?.effects() ?: tween(durationMedium, easing = easingEnter)

    fun <T> exit(): FiniteAnimationSpec<T> = springs?.effects() ?: tween(durationShort, easing = easingExit)

    /** Movement of things the user touches (sheets, the play button): springy, no overshoot drama. */
    fun <T> spatial(): SpringSpec<T> =
        springs?.spatial() ?: spring(dampingRatio = SPATIAL_DAMPING, stiffness = Spring.StiffnessMediumLow)

    private companion object {
        const val SPATIAL_DAMPING = 0.8f
    }
}

/**
 * A pair of springs in the manner of Material 3 Expressive's motion scheme: [spatial] for things
 * that move (it may overshoot a little), [effects] for color and opacity (critically damped).
 */
@Immutable
data class QItSprings(
    val spatialDamping: Float,
    val spatialStiffness: Float,
    val effectsDamping: Float,
    val effectsStiffness: Float,
) {
    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = spatialDamping, stiffness = spatialStiffness)

    fun <T> effects(): SpringSpec<T> = spring(dampingRatio = effectsDamping, stiffness = effectsStiffness)

    companion object {
        /** Material 3 Expressive's default springs (its ExpressiveMotionTokens). */
        val Expressive = QItSprings(
            spatialDamping = 0.8f,
            spatialStiffness = 380f,
            effectsDamping = 1f,
            effectsStiffness = 1600f,
        )
    }
}
