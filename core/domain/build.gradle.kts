import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin: the compiler guarantees the domain never touches Android or the data layer.
plugins {
    alias(libs.plugins.kotlin.jvm)
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

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(project(":core:testing"))
}
