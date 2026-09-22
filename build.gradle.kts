// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi) apply false
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**")
            ktlint(libs.versions.ktlint.get())
                .customRuleSets(listOf("io.nlopez.compose.rules:ktlint:${libs.versions.composeKtlintRules.get()}"))
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(libs.versions.ktlint.get())
        }
    }

    configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        parallel = true
        source.setFrom(
            "src/main/java",
            "src/main/kotlin",
            "src/test/java",
            "src/test/kotlin",
        )
    }

    // detekt 1.23.8 is compiled against Kotlin 2.0.21 and refuses to run on the build's newer
    // Kotlin ("detekt was compiled with Kotlin 2.0.21 but is currently running with ...",
    // https://detekt.dev/docs/introduction/compatibility). It only parses sources (no type
    // resolution), so its own compiler version is safe to pin.
    configurations.matching { it.name == "detekt" }.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.21")
        }
    }

    // Spotless rewrites its scratch copies under build/spotless-clean while it runs; lint's scan of
    // the module must not race it (a vanishing file fails the lint run).
    tasks.matching { it.name.startsWith("lintAnalyze") }.configureEach {
        mustRunAfter(tasks.matching { it.name.startsWith("spotless") })
    }

    // Robolectric's SDK 36 runtime (ApplicationSharedMemory) reaches into FileDescriptor internals
    // through jdk.internal.access, which java.base doesn't export to the classpath by default.
    tasks.withType<Test>().configureEach {
        jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
    }

    // One coverage setup for every module that measures coverage: the same exclusions everywhere
    // (so a module's own report and the aggregate agree), debug variant only, and the module's floors.
    plugins.withId("org.jetbrains.kotlinx.kover") {
        configure<KoverProjectExtension> {
            currentProject {
                instrumentation {
                    // Release unit tests stay uninstrumented, so no report ever needs them.
                    disabledForTestTasks.add("testReleaseUnitTest")
                }
            }
            reports {
                filters { excludeGeneratedAndGlue() }
                verify {
                    coverageFloors[path]?.let { floor ->
                        rule("$path line coverage") { minBound(floor.lines, CoverageUnit.LINE) }
                        rule("$path branch coverage") { minBound(floor.branches, CoverageUnit.BRANCH) }
                    }
                }
            }
        }
    }
}

/**
 * Minimum coverage (%) a module must keep; checked by `qualityGate`. Floors sit a little under the
 * measured values so a change can't quietly drop coverage; raise them as tests grow. The UI modules'
 * branch numbers include the Compose compiler's generated recomposition branches, hence lower floors.
 */
data class CoverageFloor(val lines: Int, val branches: Int)

val coverageFloors = mapOf(
    ":core:domain" to CoverageFloor(lines = 94, branches = 90),
    ":core:data" to CoverageFloor(lines = 89, branches = 75),
    ":core:designsystem" to CoverageFloor(lines = 94, branches = 45),
    ":app" to CoverageFloor(lines = 71, branches = 44),
    ":wear" to CoverageFloor(lines = 78, branches = 45),
)

/** Generated code, DI wiring and Android entry points: nothing of ours to unit-test. */
fun KoverReportFiltersConfig.excludeGeneratedAndGlue() {
    excludes {
        androidGeneratedClasses()
        classes(
            "*_Factory*",
            "*_MembersInjector",
            "*.Hilt_*",
            "Hilt_*",
            "*.Dagger*",
            "*_HiltModules*",
            "*_ComponentTreeDeps*",
            "*_GeneratedInjector",
            "*.di.*",
            "*.BuildConfig",
            "*.R",
            "*.R$*",
            "*ComposableSingletons*",
        )
        packages("hilt_aggregated_deps", "dagger")
        annotatedBy("androidx.compose.ui.tooling.preview.Preview")
        // Thin system glue, exercised on devices rather than by unit tests.
        inheritedFrom(
            "android.app.Activity",
            "android.app.Service",
            "android.app.Application",
        )
    }
}

// Aggregated coverage across the code-carrying modules. The Android modules disable Kover
// instrumentation for their release unit tests (see their build files), so the aggregate
// below never triggers `testReleaseUnitTest`.
dependencies {
    kover(project(":core:domain"))
    kover(project(":core:data"))
    kover(project(":core:designsystem"))
    kover(project(":app"))
    kover(project(":wear"))
}

kover {
    reports {
        // The aggregated report doesn't inherit the modules' filters.
        filters { excludeGeneratedAndGlue() }
        total {
            verify {
                rule("aggregate line coverage") { minBound(83, CoverageUnit.LINE) }
                rule("aggregate branch coverage") { minBound(58, CoverageUnit.BRANCH) }
            }
        }
    }
}

/**
 * One command that proves the codebase is formatted, statically clean, lint-clean, tested,
 * covered and architecturally sound. See docs/QUALITY.md for what each gate checks.
 */
tasks.register("qualityGate") {
    group = "verification"
    description = "Runs spotless, detekt, Android lint, all unit tests, architecture tests and coverage verification."
    dependsOn(
        // Formatting + static analysis everywhere.
        subprojects.map { "${it.path}:spotlessCheck" },
        subprojects.map { "${it.path}:detekt" },
        // Android lint on the modules that contain Android code.
        ":app:lintDebug",
        ":wear:lintDebug",
        ":core:data:lintDebug",
        ":core:designsystem:lintDebug",
        // Unit tests, including screenshot verification (debug variant only for the Android modules).
        ":core:domain:test",
        ":core:data:testDebugUnitTest",
        ":core:designsystem:testDebugUnitTest",
        ":app:testDebugUnitTest",
        ":wear:testDebugUnitTest",
        ":architecture-test:test",
        // Coverage: every module's own floors, then the aggregate.
        ":core:domain:koverVerify",
        ":core:data:koverVerifyDebug",
        ":core:designsystem:koverVerifyDebug",
        ":app:koverVerifyDebug",
        ":wear:koverVerifyDebug",
        ":koverVerify",
    )
}
