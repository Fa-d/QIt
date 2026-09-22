package dev.sadakat.qit.architecture

import org.junit.Test

/**
 * Each app uses the one Material library its form factor is designed for: Material 3 on the phone,
 * Wear Material 3 on the watch. Mixing them gives components that don't follow the app's theme.
 */
class MaterialLibraryTest {

    @Test
    fun `the watch uses Wear Material 3 only`() {
        val forbidden = listOf("androidx.wear.compose.material.", "androidx.compose.material3.")
        val violations = projectFiles()
            .filter { "/wear/src/main/" in it.path }
            .flatMap { file ->
                file.imports
                    .filter { import -> forbidden.any { import.name.startsWith(it) } }
                    .map { "${file.path}: ${it.name}" }
            }
        report(violations, "The watch must use androidx.wear.compose.material3:")
    }

    @Test
    fun `the phone uses no Wear libraries`() {
        val violations = projectFiles()
            .filter { "/app/src/main/" in it.path }
            .flatMap { file ->
                file.imports.filter { it.name.startsWith("androidx.wear.") }.map { "${file.path}: ${it.name}" }
            }
        report(violations, "The phone must not use Wear libraries:")
    }
}
