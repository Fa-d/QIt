package dev.sadakat.qit.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.Test

/**
 * Presentation code (phone + wear) talks to the domain ports only. Importing a concrete adapter
 * from `:core:data` would bypass the ports and make the UI impossible to test with fakes.
 */
class PresentationIsolationTest {

    @Test
    fun `presentation files never import the data layer`() {
        val violations = Konsist
            .scopeFromProject()
            .files
            .filter { "/build/" !in it.path }
            .filter { it.packagee?.name?.contains(".presentation") == true }
            .flatMap { file ->
                file.imports
                    .filter { it.name.startsWith("dev.sadakat.qit.core.data.") }
                    .map { "${file.path}: forbidden import ${it.name}" }
            }

        assert(violations.isEmpty()) {
            "Presentation code must depend on domain ports, not on dev.sadakat.qit.core.data:\n" +
                violations.joinToString("\n")
        }
    }

    @Test
    fun `presentation files never talk to Media3 directly`() {
        // Media3's own UI state holders bind to its Player and would bypass QuranPlayer: no ayah-wise
        // next/previous, no repeat, and nothing a fake can drive in tests.
        val violations = Konsist
            .scopeFromProject()
            .files
            .filter { "/build/" !in it.path }
            .filter { it.packagee?.name?.contains(".presentation") == true }
            .flatMap { file ->
                file.imports
                    .filter { it.name.startsWith("androidx.media3.") }
                    .map { "${file.path}: forbidden import ${it.name}" }
            }

        assert(violations.isEmpty()) {
            "Presentation code must drive playback through QuranPlayer, not Media3:\n" + violations.joinToString("\n")
        }
    }
}
