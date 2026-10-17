// Top-level build file. Plugin versions are declared here so the app module
// stays small and readable.
//
// AGP 9 has built-in Kotlin support, so the `org.jetbrains.kotlin.android`
// plugin is intentionally not applied. The Compose compiler plugin is still
// applied per module.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
