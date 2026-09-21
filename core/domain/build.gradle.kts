import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin: the compiler guarantees the domain never touches Android or the data layer.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

kover {
    reports {
        // The domain is pure logic with fast JVM tests: it must stay almost fully covered.
        verify {
            rule("domain line coverage") {
                minBound(90)
            }
        }
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(project(":core:testing"))
}
