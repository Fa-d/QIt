package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [QuranSettings]. */
class FakeQuranSettings(mode: RecitationMode = RecitationMode.ARABIC_BANGLA, lastPosition: LastPosition? = null) :
    QuranSettings {

    override val mode = MutableStateFlow(mode)
    override val lastPosition = MutableStateFlow(lastPosition)

    override suspend fun setMode(mode: RecitationMode) {
        this.mode.value = mode
    }

    override suspend fun saveLastPosition(position: LastPosition) {
        lastPosition.value = position
    }
}
