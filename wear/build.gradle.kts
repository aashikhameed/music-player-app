plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.aashik.music.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aashik.music"
        minSdk = 30
        targetSdk = 34
        versionCode = 92
        versionName = "0.4.11"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.animation:animation")
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Wear OS & Wear Compose
    implementation(libs.androidx.wear)
    implementation(libs.androidx.wear.ongoing)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.tiles.material)
    implementation(libs.androidx.wear.watchface.complications)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.wear.compose.ui.tooling)

    // Media & Icons
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.media:media:1.6.0")

    testImplementation(libs.junit)
}

tasks.matching { it.name.contains("AarMetadata") }.all { enabled = false }
