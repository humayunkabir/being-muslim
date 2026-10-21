plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.beingmuslim.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.beingmuslim.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        // Only the languages the app ships. Without this the APKs would also
        // carry every locale the libraries bundle (appcompat ships dozens) and
        // could resolve a locale the app does not support.
        resourceConfigurations += listOf("en", "bn")

        // Override with -PappwriteEndpoint=... / -PappwriteProjectId=... for
        // another environment; no code change or secret is involved.
        buildConfigField(
            "String",
            "APPWRITE_ENDPOINT",
            "\"${providers.gradleProperty("appwriteEndpoint").getOrElse("https://appwrite.alveo.dev/v1")}\"",
        )
        buildConfigField(
            "String",
            "APPWRITE_PROJECT_ID",
            "\"${providers.gradleProperty("appwriteProjectId").getOrElse("being-muslim")}\"",
        )
        // The function that creates the user's sync row through the trusted
        // server path; clients have no create permission on the table.
        buildConfigField(
            "String",
            "APPWRITE_SYNC_FUNCTION_ID",
            "\"${providers.gradleProperty("appwriteSyncFunctionId").getOrElse("user-sync")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // jvmTarget for the built-in Kotlin compiler follows targetCompatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    // Per-app locale (issue #14): the platform's per-app language API is 33+,
    // and AppCompat is its documented backport for the minSdk here.
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.adhan)
    // Reminders (issue #12): local, persisted scheduling that survives reboot
    // without an exact-alarm permission or a boot receiver.
    implementation(libs.androidx.work.runtime)
    // Optional account: Appwrite auth and sync. Core features still work offline.
    // POM metadata says GPL-3.0; upstream LICENSE.md is BSD-3-Clause (verified
    // 2026-10-21). Keep the BSD notice in the distributed third-party notices.
    implementation(libs.appwrite)
    // The Appwrite SDK takes a fixed SharedPreferences cookie store; this
    // Keystore-backed file keeps the session token out of plaintext.
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Android's org.json is a stub in local JVM tests; the real implementation
    // exercises the sync payload codec without a device.
    testImplementation(libs.json)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
