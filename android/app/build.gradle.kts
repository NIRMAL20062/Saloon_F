// Glide Android app. AGP 9 compiles Kotlin itself (built-in Kotlin), so there is no kotlin-android plugin.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.glide.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.glide.android"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        // Supabase project for logins (D-016). Both values are public by design (DF-19): the publishable key only lets
        // the app ask Supabase to send and check OTPs. Secret keys never go into the app.
        buildConfigField("String", "SUPABASE_URL", "\"https://uwvaebgbdqitbnymoqfq.supabase.co/\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"sb_publishable_4SjiEzyCa5wA7sQQETlNtA_Gq0gQ7R_\"")
    }

    buildTypes.configureEach {
        // The only thing the app knows about the backend is its URL. No keys or secrets, ever.
        val apiBaseUrl =
            when (name) {
                // Phone/emulator reach the laptop via `adb reverse tcp:8080 tcp:8080` (DF-10).
                "debug" -> "http://127.0.0.1:8080/"

                // Placeholder until hosting is chosen (Q-004). `.invalid` can never resolve.
                else -> "https://api.glide.invalid/"
            }
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            // R8: shrinks and obfuscates release builds. Signing is added by CD (BE-013) from GitHub secrets.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Robolectric needs merged resources to render Compose UI tests on the JVM.
            isIncludeAndroidResources = true
            // Robolectric reaches into JDK internals that JDK 24+ locks down (Android Studio bundles JDK 25).
            all { it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED") }
        }
    }

    lint {
        abortOnError = true
        checkDependencies = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit4)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
