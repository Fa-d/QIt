plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi)
}

val qandeelVersionCode = providers.gradleProperty("qandeel.versionCode").get().toInt()

android {
    namespace = "dev.sadakat.qandeel.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.sadakat.qandeel"
        minSdk = 26
        targetSdk = 36
        // gradle.properties holds the release; the watch app's versionCode ends in 1 (see there).
        versionCode = qandeelVersionCode * 10 + 1
        versionName = providers.gradleProperty("qandeel.versionName").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // The upload key from the root build (keystore.properties or QIT_UPLOAD_*), when present.
        @Suppress("UNCHECKED_CAST")
        (rootProject.extra["uploadSigning"] as Map<String, String>?)?.let { key ->
            create("release") {
                storeFile = file(key.getValue("storeFile"))
                storePassword = key.getValue("storePassword")
                keyAlias = key.getValue("keyAlias")
                keyPassword = key.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    // Built-in Kotlin takes its jvmTarget from targetCompatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        compose = true
    }
    // The watch never shows word-by-word meanings: aapt keeps the quran/words assets (4 MB) out of
    // its APK (the phone bundles them).
    androidResources {
        ignoreAssetsPatterns += "<dir>words"
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = true
        warningsAsErrors = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

hilt {
    enableAggregatingTask = false
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Wear Compose (Material 3; lists from the foundation package)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Tile (protolayout, Material 3 tile layouts)
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.wear.protolayout.material3)
    debugImplementation(libs.wear.tiles.renderer)
    debugImplementation(libs.wear.tiles.tooling.preview)

    // Wearable Data Layer
    implementation(libs.play.services.wearable)

    // Media3 for audio playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Testing: JVM unit tests (fakes from :core:testing) and Robolectric Compose UI tests
    testImplementation(project(":core:testing"))
    // Hilt-injected Android entry points (services) under Robolectric
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.media3.test.utils)
    testImplementation(libs.media3.test.utils.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.wear.tiles.testing)
    // Screenshot tests (goldens in src/test/screenshots) with accessibility checks
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.accessibility.check)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
