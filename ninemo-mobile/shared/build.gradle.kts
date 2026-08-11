
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)    // com.android.kotlin.multiplatform.library (AGP 9+)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.skie)
}

kotlin {
    // AGP 9+: android {} inside kotlin {} replaces the old androidTarget {} + top-level android {}.
    // namespace, compileSdk, minSdk all live here with com.android.kotlin.multiplatform.library.
    android {
        namespace = "com.reejuven8.ninemo.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(libs.androidx.lifecycle.viewmodel)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // api: JsonElement/JsonObject types appear in public DTOs (e.g. TimelineResponse),
            // so consuming modules (androidApp) need this on their compile classpath too.
            api(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.krossbow.stomp)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.security.crypto)
            implementation(libs.krossbow.websocket.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.krossbow.websocket.ktor)
        }
    }
}

skie {
    features {
        enableSwiftUIObservingPreview = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
