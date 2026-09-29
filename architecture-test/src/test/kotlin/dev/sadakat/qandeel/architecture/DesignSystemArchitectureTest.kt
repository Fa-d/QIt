package dev.sadakat.qandeel.architecture

import org.junit.Test
import java.io.File

/**
 * The design system is shared by a Material 3 phone and a Wear Material 3 watch, so it depends on
 * Compose UI only. Its reference palettes are read by theme code alone: screens pick semantic roles.
 * It also owns the app's single font source.
 */
class DesignSystemArchitectureTest {

    private val designSystem = "dev.sadakat.qandeel.core.designsystem"

    @Test
    fun `the design system depends on neither Material library, Wear, nor the app's layers`() {
        val forbidden = listOf(
            "androidx.compose.material.",
            "androidx.compose.material3.",
            "androidx.wear.",
            "dev.sadakat.qandeel.core.domain.",
            "dev.sadakat.qandeel.core.data.",
        )
        val violations = projectFiles()
            .filter { it.packagee?.name?.startsWith(designSystem) == true }
            .flatMap { file ->
                file.imports
                    .filter { import -> forbidden.any { import.name.startsWith(it) } }
                    .map { "${file.path}: forbidden import ${it.name}" }
            }
        report(violations, "The design system must stay Compose-UI only:")
    }

    @Test
    fun `only theme code reads the reference palettes`() {
        val allowed =
            listOf(designSystem, "dev.sadakat.qandeel.ui.theme", "dev.sadakat.qandeel.wear.presentation.theme")
        val violations = projectFiles()
            .filter { "/src/main/" in it.path }
            .filterNot { file -> allowed.any { file.packagee?.name?.startsWith(it) == true } }
            .flatMap { file ->
                file.imports
                    .filter { it.name.startsWith("$designSystem.ref.") }
                    .map { "${file.path}: ${it.name} — use a semantic role from QandeelTheme.colors" }
            }
        report(violations, "Reference tokens used outside theme code:")
    }

    @Test
    fun `fonts live in the design system only`() {
        val fontDirs = listOf("app", "wear").map { File(projectRoot, "$it/src/main/res/font") }.filter { it.exists() }
        val fontRefs = appMainFiles()
            .filter { "R.font." in it.code }
            .map { "${it.path}: R.font — use QandeelFonts" }
        report(
            fontDirs.map {
                "${it.path}: move fonts to core/designsystem"
            } + fontRefs,
            "Fonts outside the design system:",
        )
    }
}
