// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover)
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

    // detekt 1.23.7 is compiled against Kotlin 2.0.10 while this build uses KGP 2.0.0; without
    // this force detekt fails with "detekt was compiled with Kotlin 2.0.10 but is currently
    // running with 2.0.0" (https://detekt.dev/docs/introduction/compatibility).
    configurations.matching { it.name == "detekt" }.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.10")
        }
    }
}

// Aggregated coverage across the code-carrying modules. The Android modules disable Kover
// instrumentation for their release unit tests (see their build files), so the aggregate
// below never triggers `testReleaseUnitTest`.
dependencies {
    kover(project(":core:domain"))
    kover(project(":core:data"))
    kover(project(":app"))
    kover(project(":wear"))
}

kover {
    reports {
        total {
            verify {
                rule("aggregate line coverage") {
                    bound { minValue = 70 }
                }
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
        // Unit tests (debug variant only for the Android modules).
        ":core:domain:test",
        ":core:data:testDebugUnitTest",
        ":app:testDebugUnitTest",
        ":wear:testDebugUnitTest",
        ":architecture-test:test",
        // Root Kover verification (aggregated 70% + rules configured above).
        ":koverVerify",
    )
}
