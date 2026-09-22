pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QIt"
include(":app")
include(":wear")
include(":core:domain")
include(":core:designsystem")
include(":core:data")
include(":core:testing")
include(":architecture-test")
include(":baselineprofile")
