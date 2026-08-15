

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
kotlin {
    jvmToolchain(21)
}

val signingStoreFile = providers.gradleProperty("SIGNING_STORE_FILE")
    .orElse(providers.environmentVariable("SIGNING_STORE_FILE"))
val signingStorePassword = providers.gradleProperty("SIGNING_STORE_PASSWORD")
    .orElse(providers.environmentVariable("SIGNING_STORE_PASSWORD"))
val signingKeyAlias = providers.gradleProperty("SIGNING_KEY_ALIAS")
    .orElse(providers.environmentVariable("SIGNING_KEY_ALIAS"))
val signingKeyPassword = providers.gradleProperty("SIGNING_KEY_PASSWORD")
    .orElse(providers.environmentVariable("SIGNING_KEY_PASSWORD"))

val hasCustomSigning = listOf(
    signingStoreFile.orNull,
    signingStorePassword.orNull,
    signingKeyAlias.orNull,
    signingKeyPassword.orNull
).all { !it.isNullOrBlank() }

val ciVersionName = providers.gradleProperty("CI_VERSION_NAME")
    .orElse(providers.environmentVariable("CI_VERSION_NAME"))
    .orNull
val ciVersionCode = providers.gradleProperty("CI_VERSION_CODE")
    .orElse(providers.environmentVariable("CI_VERSION_CODE"))
    .orNull
    ?.toIntOrNull()

android {
    namespace = "com.vinithreddybanda.whatsapstatus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vinithreddybanda.whatsapstatus"
        minSdk = 24
        targetSdk = 36
        versionCode = ciVersionCode ?: 2
        versionName = ciVersionName ?: "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasCustomSigning) {
                storeFile = file(signingStoreFile.get())
                storePassword = signingStorePassword.get()
                keyAlias = signingKeyAlias.get()
                keyPassword = signingKeyPassword.get()
            }
        }
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("github") {
            dimension = "distribution"
            if (hasCustomSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        create("fdroid") {
            dimension = "distribution"
            versionNameSuffix = "-fdroid"
            resValue("string", "app_name", "WhatsApStatusSaver F-Droid")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
