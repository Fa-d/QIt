package dev.sadakat.qit.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import org.junit.Test

/**
 * UiState types are the contract between a ViewModel and its composables: cheap immutable
 * snapshots. They must be data (or sealed) classes and must not expose anything mutable.
 */
class UiStateArchitectureTest {

    @Test
    fun `classes named UiState are data classes or sealed classes`() {
        val violations = uiStates()
            .filterNot { it.hasDataModifier || it.hasSealedModifier }
            .map { "${it.containingFile.path}: ${it.name} must be a data class or sealed class" }

        assert(violations.isEmpty()) {
            "*UiState types must be data classes (or sealed hierarchies):\n" +
                violations.joinToString("\n")
        }
    }

    @Test
    fun `classes named UiState declare only val properties`() {
        val violations = uiStates()
            .flatMap { uiState ->
                (uiState.properties() + constructorParameters(uiState))
                    .filter { it.isVar }
                    .map { property ->
                        "${uiState.containingFile.path}: ${uiState.name}.${property.name} " +
                            "must be a val"
                    }
            }

        assert(violations.isEmpty()) {
            "*UiState types must expose only `val` properties:\n${violations.joinToString("\n")}"
        }
    }

    private fun uiStates() = Konsist
        .scopeFromProject()
        .files
        .filter { "/build/" !in it.path }
        .flatMap { it.classes() }
        .filter { it.name.endsWith("UiState") }

    private fun constructorParameters(uiState: KoClassDeclaration) = uiState.primaryConstructor?.parameters.orEmpty()
}
