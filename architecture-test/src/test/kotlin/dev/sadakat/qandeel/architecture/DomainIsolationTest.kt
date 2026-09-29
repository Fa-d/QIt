package dev.sadakat.qandeel.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.Test

/**
 * The domain layer must stay a pure Kotlin island: no Android, no androidx, no access to the
 * layers that implement its ports. This keeps `:core:domain` buildable without the Android SDK
 * and forces every platform concern behind a port interface.
 */
class DomainIsolationTest {

    @Test
    fun `domain files import nothing from android, androidx, the data layer or presentation`() {
        val forbiddenImportPrefixes = listOf(
            "android.",
            "androidx.",
            "dev.sadakat.qandeel.core.data.",
            "dev.sadakat.qandeel.presentation.",
            "dev.sadakat.qandeel.wear.",
        )

        val violations = Konsist
            .scopeFromProject()
            .files
            .filter { "/build/" !in it.path }
            .filter { it.packagee?.name?.startsWith("dev.sadakat.qandeel.core.domain") == true }
            .flatMap { file ->
                file.imports
                    .filter { import -> forbiddenImportPrefixes.any { import.name.startsWith(it) } }
                    .map { "${file.path}: forbidden import ${it.name}" }
            }

        assert(violations.isEmpty()) {
            "Domain layer must not import Android or outer layers:\n${violations.joinToString("\n")}"
        }
    }
}
