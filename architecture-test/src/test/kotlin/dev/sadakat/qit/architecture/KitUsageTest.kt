package dev.sadakat.qit.architecture

import org.junit.Test

/**
 * The phone's screens are built from the UI kit (`:core:ui`), which draws every surface from the
 * current look's tokens; that is what makes a look (mushaf, material, expressive, glass) a set of
 * tokens rather than code in every screen. So screens may use Material 3 only for things that
 * carry no surface of their own (text, icons, buttons, sliders, progress, ...), and the containers
 * (bars, cards, sheets, dialogs, menus, fields, segmented buttons) come from the kit.
 */
class KitUsageTest {

    private val allowedMaterial = setOf(
        "Text", "Icon", "IconButton", "IconToggleButton", "IconButtonDefaults", "FilledIconButton",
        "FilledTonalIconButton", "TextButton", "Button", "FilledTonalButton", "OutlinedButton", "ButtonDefaults",
        "MaterialTheme", "LocalContentColor", "LocalTextStyle", "ProvideTextStyle", "contentColorFor",
        "CircularProgressIndicator", "LinearProgressIndicator", "ProgressIndicatorDefaults",
        "Slider", "SliderDefaults", "RangeSlider", "Switch", "Checkbox", "RadioButton",
        "HorizontalDivider", "VerticalDivider", "DropdownMenuItem", "Badge", "BadgedBox",
        "FilterChip", "FilterChipDefaults", "AssistChip", "AssistChipDefaults",
        "SnackbarHost", "SnackbarHostState", "SnackbarResult", "SnackbarDuration",
        "ExperimentalMaterial3Api",
    )

    private val themePackages = listOf("dev.sadakat.qit.ui.theme", "dev.sadakat.qit.core.ui.theme")

    @Test
    fun `screens build their containers from the UI kit`() {
        val violations = phoneScreens()
            .flatMap { file ->
                file.imports
                    .map { it.name }
                    .filter { it.startsWith(MATERIAL3) }
                    .filterNot { it.removePrefix(MATERIAL3) in allowedMaterial }
                    .map { "${file.path}: $it — use the :core:ui kit (QItTopBar, QItCard, QItSheet, ...)" }
            }
        report(violations, "Material containers used directly in screens:")
    }

    @Test
    fun `nothing star-imports Material 3`() {
        val violations = projectFiles()
            .flatMap { file ->
                file.imports.filter { it.name == "$MATERIAL3*" || it.name == "androidx.compose.material3" }
                    .map { "${file.path}: ${it.name}" }
            }
        report(violations, "Star imports of Material 3 (the kit rule can't see through them):")
    }

    @Test
    fun `only the kit's glass talks to Haze`() {
        val violations = projectFiles()
            .filterNot { it.packagee?.name == "dev.sadakat.qit.core.ui.kit.glass" }
            .flatMap { file ->
                file.imports.filter { it.name.startsWith("dev.chrisbanes.haze") }.map { "${file.path}: ${it.name}" }
            }
        report(violations, "Haze used outside core.ui.kit.glass:")
    }

    @Test
    fun `only theme code builds a MaterialTheme`() {
        val violations = (appMainFiles() + uiKitMainFiles())
            .filter { "/app/src/main/" in it.path || "/core/ui/src/main/" in it.path }
            .filterNot { file -> themePackages.any { file.packagee?.name == it } }
            .filter { Regex("""\bMaterialTheme\s*\(""").containsMatchIn(it.code) }
            .map { "${it.path}: MaterialTheme(...) — wrap content in QItAppTheme instead" }
        report(violations, "MaterialTheme built outside the theme:")
    }

    @Test
    fun `components never ask which style is on`() {
        // A look is its tokens: the kit and the screens read tokens, and only the theme and the
        // appearance settings (which offer the styles) may name a style.
        val allowed = listOf(
            "dev.sadakat.qit.ui.theme",
            "dev.sadakat.qit.core.ui.theme",
            "dev.sadakat.qit.presentation.appearance",
            "dev.sadakat.qit.presentation.settings",
        )
        val styleTypes = listOf("QItStyle", "UiStyle")
        val violations = (phoneScreens() + uiKitMainFiles())
            .filterNot { file -> allowed.any { file.packagee?.name?.startsWith(it) == true } }
            .filterNot { it.name == "AppViewModel" }
            .filter { file -> styleTypes.any { Regex("""\b$it\b""").containsMatchIn(file.code) } }
            .map { "${it.path}: reads the style — read QItTheme tokens instead" }
        report(violations, "Code that branches on the style:")
    }

    private fun phoneScreens() = appMainFiles()
        .filter { "/app/src/main/" in it.path }
        .filter { it.packagee?.name?.startsWith("dev.sadakat.qit.presentation") == true }

    private companion object {
        const val MATERIAL3 = "androidx.compose.material3."
    }
}
