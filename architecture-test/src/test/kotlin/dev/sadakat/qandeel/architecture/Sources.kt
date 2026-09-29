package dev.sadakat.qandeel.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import java.io.File

/** Every Kotlin source file of the project, generated sources excluded. */
internal fun projectFiles(): List<KoFileDeclaration> = Konsist
    .scopeFromProject()
    .files
    .filter { "/build/" !in it.path }

/** Production sources of the phone and watch apps. */
internal fun appMainFiles(): List<KoFileDeclaration> =
    projectFiles().filter { "/app/src/main/" in it.path || "/wear/src/main/" in it.path }

/** Production sources of the phone's UI kit (`:core:ui`). */
internal fun uiKitMainFiles(): List<KoFileDeclaration> = projectFiles().filter { "/core/ui/src/main/" in it.path }

/** The file's code with comments and string literals blanked out, so rules only see code. */
internal val KoFileDeclaration.code: String
    get() = text
        .replace(Regex("""/\*[\s\S]*?\*/"""), "")
        .replace(Regex("""//[^\n]*"""), "")
        .replace(Regex("\"\"\"[\\s\\S]*?\"\"\""), "\"\"")
        .replace(Regex(""""(?:\\.|[^"\\\n])*""""), "\"\"")

/** The repository root (tests run from the module directory). */
internal val projectRoot: File = File(System.getProperty("user.dir")).parentFile

internal fun report(violations: List<String>, message: String) {
    assert(violations.isEmpty()) { "$message\n${violations.joinToString("\n")}" }
}
