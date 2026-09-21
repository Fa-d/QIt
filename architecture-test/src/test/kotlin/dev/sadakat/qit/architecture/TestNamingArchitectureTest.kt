package dev.sadakat.qit.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.Test

/**
 * Test classes are named after the behaviour they verify, so every class declared in a test
 * source set ends with `Test`. `:core:testing` is the shared test-fixtures module (its sources
 * are fakes, not tests), so it is exempt.
 */
class TestNamingArchitectureTest {

    @Test
    fun `classes declared in test source sets are named ending with Test`() {
        val violations = Konsist
            .scopeFromProject()
            .files
            .filter { "/build/" !in it.path }
            .filter { "/src/test/" in it.path }
            .filterNot { it.path.contains("/core/testing/") }
            .flatMap { it.classes() }
            .filterNot { it.name.endsWith("Test") }
            .map { "${it.containingFile.path}: ${it.name} must end with Test" }

        assert(violations.isEmpty()) {
            "Test classes must be suffixed with Test:\n${violations.joinToString("\n")}"
        }
    }
}
