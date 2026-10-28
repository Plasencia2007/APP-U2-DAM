import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.androidx.security.crypto)
    implementation(libs.sqldelight.android)
    implementation(libs.sqldelight.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.koin.core)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "pe.upeu.ecorecicla"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pe.upeu.ecorecicla"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        // Direccion base del servicio. 10.0.2.2 es el localhost de la PC vista desde el emulador.
        buildConfigField("String", "URL_BASE", "\"http://10.0.2.2:8080/api/v1/\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            buildConfigField("Boolean", "REGISTRO_DETALLADO", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("Boolean", "REGISTRO_DETALLADO", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
