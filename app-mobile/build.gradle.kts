import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Optional stable signing key, same mechanism as app-tv (see README).
val nexusKeystoreFile: String? = System.getenv("NEXUS_KEYSTORE_FILE")
val nexusKeystorePassword: String? = System.getenv("NEXUS_KEYSTORE_PASSWORD")
val useNexusKey = !nexusKeystoreFile.isNullOrBlank() && !nexusKeystorePassword.isNullOrBlank()

android {
    namespace = "com.nexus.personalintelligence.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nexus.personalintelligence.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-release1"
    }

    signingConfigs {
        if (useNexusKey) {
            create("nexus") {
                storeFile = file(nexusKeystoreFile!!)
                storePassword = nexusKeystorePassword
                keyAlias = "nexus"
                keyPassword = nexusKeystorePassword
                storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".r2debug"
            signingConfigs.findByName("nexus")?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:remote"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
