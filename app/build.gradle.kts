plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// GitHub Actions creates an ephemeral test-signing key so the APK can be installed.
// This key is not suitable for Play Store distribution or signing future updates.
val ciSigningStore = System.getenv("STORE_FILE")
val ciSigningPassword = System.getenv("STORE_PASSWORD")
val ciSigningAlias = System.getenv("KEY_ALIAS")
val ciSigningKeyPassword = System.getenv("KEY_PASSWORD")

android {
    namespace = "com.kagglecontroller"
    compileSdk = 35

    signingConfigs {
        if (!ciSigningStore.isNullOrBlank()) {
            create("ciTest") {
                storeFile = file(ciSigningStore)
                storePassword = ciSigningPassword ?: ""
                keyAlias = ciSigningAlias ?: ""
                keyPassword = ciSigningKeyPassword ?: ""
            }
        }
    }

    defaultConfig {
        applicationId = "com.kagglecontroller"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            if (!ciSigningStore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("ciTest")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work)
    implementation(libs.androidx.browser)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
