package dev.sadakat.qit.wear.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [StreamVolume] that records the crown steps tests send it. */
class FakeStreamVolume(level: Float = 1f) : StreamVolume {

    private val levelState = MutableStateFlow(level)
    override val level: StateFlow<Float> = levelState

    val steps = mutableListOf<Int>()

    override fun adjust(steps: Int) {
        this.steps += steps
        levelState.value = (levelState.value + steps * STEP).coerceIn(0f, 1f)
    }

    private companion object {
        const val STEP = 0.1f
    }
}
