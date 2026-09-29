package dev.sadakat.qandeel.core.designsystem.scale

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QandeelMotionTest {

    private val motion = QandeelMotion()

    @Test
    fun `entering takes the medium duration with the enter easing`() {
        val spec = motion.enter<Float>() as TweenSpec
        assertEquals(motion.durationMedium, spec.durationMillis)
        assertEquals(motion.easingEnter, spec.easing)
    }

    @Test
    fun `leaving is quicker than entering`() {
        val exit = motion.exit<Float>() as TweenSpec
        assertEquals(motion.durationShort, exit.durationMillis)
        assertEquals(motion.easingExit, exit.easing)
        assertTrue(exit.durationMillis < (motion.enter<Float>() as TweenSpec).durationMillis)
    }

    @Test
    fun `standard changes use the standard easing`() {
        val spec = motion.standard<Float>() as TweenSpec
        assertEquals(motion.easingStandard, spec.easing)
    }

    @Test
    fun `touch-driven movement is a spring that settles without bouncing much`() {
        val spec = motion.spatial<Float>() as SpringSpec
        assertTrue(spec.dampingRatio >= 0.75f)
    }

    @Test
    fun `with springs every spec is a spring, spatial ones from the spatial pair`() {
        val springy = QandeelMotion(springs = QandeelSprings.Expressive)

        listOf(springy.standard<Float>(), springy.enter(), springy.exit()).forEach { spec ->
            spec as SpringSpec
            assertEquals(QandeelSprings.Expressive.effectsDamping, spec.dampingRatio)
            assertEquals(QandeelSprings.Expressive.effectsStiffness, spec.stiffness)
        }
        val spatial = springy.spatial<Float>()
        assertEquals(QandeelSprings.Expressive.spatialDamping, spatial.dampingRatio)
        assertEquals(QandeelSprings.Expressive.spatialStiffness, spatial.stiffness)
    }
}
