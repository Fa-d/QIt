// The phone's UI kit: QIt's tokens mapped onto Material 3, and the components screens are built
// from. Every look (mushaf, material, expressive, glass) comes from the tokens a skin provides, so
// screens never ask which style is on. The one place Haze (backdrop blur) is used.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "dev.sadakat.qit.core.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    // Built-in Kotlin takes its jvmTarget from targetCompatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Screenshots through the hardware renderer, so RenderEffect runs and glass is really
        // blurred (the software canvas would show the unblurred page).
        unitTests.all { it.systemProperty("robolectric.pixelCopyRenderMode", "hardware") }
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = true
        warningsAsErrors = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    api(project(":core:designsystem"))
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.haze)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.accessibility.check)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
