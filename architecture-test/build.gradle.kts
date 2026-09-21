import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Architecture rules enforced on the whole codebase (Konsist). Pure JVM: it only reads sources.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.junit)
}
