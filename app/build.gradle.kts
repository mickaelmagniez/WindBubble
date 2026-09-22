import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Release signing is optional: F-Droid signs its own builds, and a plain `assembleRelease` without
 * credentials still produces an (unsigned) APK. Values come from `keystore.properties` locally and
 * from environment variables in CI.
 */
val releaseSigning: Map<String, String>? = run {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) {
        val properties = Properties().apply { propertiesFile.inputStream().use(::load) }
        listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
            .associateWith { properties.getProperty(it) ?: return@run null }
    } else {
        val fromEnvironment = mapOf(
            "storeFile" to providers.environmentVariable("SIGNING_KEYSTORE_PATH").orNull,
            "storePassword" to providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull,
            "keyAlias" to providers.environmentVariable("SIGNING_KEY_ALIAS").orNull,
            "keyPassword" to providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull,
        )
        if (fromEnvironment.values.any { it == null }) null else fromEnvironment.mapValues { it.value!! }
    }
}

android {
    namespace = "io.github.mickaelmagniez.windbubble"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.mickaelmagniez.windbubble"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        releaseSigning?.let { credentials ->
            create("release") {
                storeFile = file(credentials.getValue("storeFile"))
                storePassword = credentials.getValue("storePassword")
                keyAlias = credentials.getValue("keyAlias")
                keyPassword = credentials.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    dependenciesInfo {
        // F-Droid and reproducible-build checks reject the encrypted Play dependency blob.
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        jniLibs {
            // Stripping depends on whether an NDK is installed (the GitHub runner has one, F-Droid's
            // build server does not), which would make the APKs differ. Ship the libraries as-is.
            keepDebugSymbols += "**/*.so"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// The generated baseline profile (assets/dexopt/baseline.prof) is not byte-for-byte stable between
// builds, which breaks F-Droid's reproducible-build check against the published APK.
tasks.matching { "ArtProfile" in it.name }.configureEach {
    enabled = false
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
