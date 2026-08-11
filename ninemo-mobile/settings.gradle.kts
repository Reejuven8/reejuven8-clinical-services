rootProject.name = "ninemo-mobile"

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // SKIE plugin artifacts
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // org.jetbrains.androidx.* KMP artifacts (lifecycle-viewmodel, etc.)
        // Required by Koin 4.x and other KMP-first libraries
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":shared")
include(":androidApp")
