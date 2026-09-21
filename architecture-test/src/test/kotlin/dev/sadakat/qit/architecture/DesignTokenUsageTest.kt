package dev.sadakat.qit.architecture

import org.junit.Test

/**
 * App code draws with design tokens (`QItTheme.colors`, `QItTheme.spacing`, `MaterialTheme.typography`
 * ...), never with literal colors or sizes: one place decides how QIt looks, and light, dark, dynamic
 * color and the text-size setting all keep working. The theme packages, which map the tokens onto
 * Material, are the exception.
 */
class DesignTokenUsageTest {

    private val themePackages = listOf("dev.sadakat.qit.ui.theme", "dev.sadakat.qit.wear.presentation.theme")
    private val colorLiteral = Regex("""\bColor\s*\(""")

    // A number followed by .dp or .sp, e.g. 16.dp or 1.5.sp; 0.dp ("none") is allowed.
    private val sizeLiteral = Regex("""(?<![\w.])(\d+(?:\.\d+)?)f?\.(dp|sp)\b""")

    @Test
    fun `app code uses color tokens, not color literals`() {
        val violations = tokenFiles().flatMap { file ->
            colorLiteral.findAll(file.code).map {
                "${file.path}: ${it.value}… — use QItTheme.colors or MaterialTheme.colorScheme"
            }
        }
        report(violations, "Literal colors in app code:")
    }

    @Test
    fun `app code uses size tokens, not dp or sp literals`() {
        val violations = tokenFiles().flatMap { file ->
            sizeLiteral.findAll(file.code)
                .filterNot { it.groupValues[1].toDouble() == 0.0 }
                .map { "${file.path}: ${it.value} — use QItTheme.spacing/sizes/radius or a component token" }
        }
        report(violations, "Literal sizes in app code:")
    }

    private fun tokenFiles() = appMainFiles()
        .filterNot { it.isLegacyUi }
        .filterNot { file -> themePackages.any { file.packagee?.name == it } }
}
