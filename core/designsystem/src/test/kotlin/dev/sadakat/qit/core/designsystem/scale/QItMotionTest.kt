package dev.sadakat.qit.core.designsystem.scale

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QItMotionTest {

    private val motion = QItMotion()

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
}
