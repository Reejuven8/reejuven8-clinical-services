plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.skie) apply false
}

// Koin 4.x transitively depends on org.jetbrains.androidx.lifecycle:lifecycle-viewmodel
// which is NOT published at 2.8.x under that groupId. Redirect to the canonical
// androidx.lifecycle groupId that IS available on Google Maven for all KMP targets.
subprojects {
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            listOf(
                "lifecycle-viewmodel",
                "lifecycle-viewmodel-savedstate",
                "lifecycle-runtime",
                "lifecycle-common",
            ).forEach { artifact ->
                substitute(module("org.jetbrains.androidx.lifecycle:$artifact"))
                    .using(module("androidx.lifecycle:$artifact:2.8.7"))
            }
        }
    }
}
