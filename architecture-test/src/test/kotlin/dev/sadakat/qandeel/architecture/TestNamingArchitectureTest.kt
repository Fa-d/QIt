package dev.sadakat.qandeel.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.Test

/**
 * Test classes are named after the behaviour they verify, so every non-private class declared in a
 * test source set ends with `Test`, unless it is a test double named for its role (`Fake*`,
 * `Stub*`, `Recording*`, `Spy*`). Private helpers inside a test are the test's own business.
 * `:core:testing` is the shared test-fixtures module (its sources are fakes, not tests), so it is
 * exempt.
 */
class TestNamingArchitectureTest {

    private val testDoublePrefixes = listOf("Fake", "Stub", "Recording", "Spy")

    @Test
    fun `classes declared in test source sets are tests or named test doubles`() {
        val violations = Konsist
            .scopeFromProject()
            .files
            .filter { "/build/" !in it.path }
            .filter { "/src/test/" in it.path }
            .filterNot { it.path.contains("/core/testing/") }
            .flatMap { it.classes() }
            .filterNot { it.hasPrivateModifier }
            .filterNot { it.name.endsWith("Test") }
            .filterNot { klass -> testDoublePrefixes.any { klass.name.startsWith(it) } }
            .map { "${it.containingFile.path}: ${it.name} must end with Test or be a Fake/Stub/Recording/Spy" }

        assert(violations.isEmpty()) {
            "Misnamed classes in test sources:\n${violations.joinToString("\n")}"
        }
    }
}
