package dev.sadakat.qit.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import org.junit.Test

/**
 * Every ViewModel follows the same shape: Hilt-injected, constructed without platform types,
 * and exposing immutable state only (no public `MutableStateFlow`/`MutableSharedFlow` that the
 * UI could write to behind unidirectional data flow's back).
 */
class ViewModelArchitectureTest {

    @Test
    fun `classes named ViewModel live in a presentation package`() {
        val violations = viewModels()
            .filterNot { it.resideInPackage("..presentation..") }
            .map { "${it.containingFile.path} must be moved into a presentation package" }

        assert(violations.isEmpty()) {
            "ViewModels must live in ..presentation.. packages:\n${violations.joinToString("\n")}"
        }
    }

    @Test
    fun `classes named ViewModel are annotated with HiltViewModel`() {
        val violations = viewModels()
            .filterNot {
                it.hasAnnotationWithName("HiltViewModel", "dagger.hilt.android.lifecycle.HiltViewModel")
            }
            .map { "${it.containingFile.path}: ${it.name} is missing @HiltViewModel" }

        assert(violations.isEmpty()) {
            "ViewModels must be annotated with @HiltViewModel:\n${violations.joinToString("\n")}"
        }
    }

    @Test
    fun `viewModels are not constructed with a Context or an Application`() {
        val forbiddenTypes = setOf("Context", "Application")

        val violations = viewModels()
            .flatMap { viewModel ->
                constructorParameters(viewModel)
                    .filter { it.type.name in forbiddenTypes }
                    .map { parameter ->
                        "${viewModel.containingFile.path}: ${viewModel.name} constructor takes " +
                            "${parameter.type.name} (${parameter.name})"
                    }
            }

        assert(violations.isEmpty()) {
            "ViewModels must not take Context/Application constructor parameters " +
                "(inject an adapter instead):\n${violations.joinToString("\n")}"
        }
    }

    @Test
    fun `viewModels expose no non-private mutable flows`() {
        val mutableFlowTypes = setOf("MutableStateFlow", "MutableSharedFlow")

        val violations = viewModels()
            .flatMap { viewModel ->
                // properties() includes constructor `val`/`var` parameters, so both declaration
                // sites are covered. Generic types report their arguments (MutableStateFlow<Int>).
                viewModel.properties()
                    .filter { it.type?.name?.substringBefore("<") in mutableFlowTypes }
                    .filterNot { it.hasPrivateModifier }
                    .map { property ->
                        "${viewModel.containingFile.path}: ${viewModel.name}.${property.name} " +
                            "is a non-private ${property.type?.name}"
                    }
            }

        assert(violations.isEmpty()) {
            "Mutable flows in ViewModels must be private:\n${violations.joinToString("\n")}"
        }
    }

    private fun viewModels(): List<KoClassDeclaration> = Konsist
        .scopeFromProject()
        .files
        .filter { "/build/" !in it.path }
        .flatMap { it.classes() }
        .filter { it.name.endsWith("ViewModel") }

    private fun constructorParameters(viewModel: KoClassDeclaration) = viewModel.constructors.flatMap { it.parameters }
}
