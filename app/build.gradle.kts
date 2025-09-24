// build.gradle (App level with version catalog plugins)
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.ksp)
    id("kotlin-kapt")
}

android {
    namespace = "com.aashik.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aashik.music"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
val signingKeystorePath = System.getenv("KEYSTORE_PATH") ?: throw GradleException("KEYSTORE_PATH not set")
    val signingKeystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: throw GradleException("KEYSTORE_PASSWORD not set")
        val signingKeyAlias = System.getenv("KEY_ALIAS") ?: throw GradleException("KEY_ALIAS not set")
            val signingKeyPassword = System.getenv("KEY_PASSWORD") ?: throw GradleException("KEY_PASSWORD not set")


                signingConfigs {
                            create("release") {
                                            storeFile = file(signingKeystorePath)
                                                        storePassword = signingKeystorePassword
                                                                    keyAlias = signingKeyAlias
                                                                                keyPassword = signingKeyPassword
                            }
                }

                    buildTypes {
                                release {
                                                isMinifyEnabled = true
                                                            isShrinkResources = true
                                                                        signingConfig = signingConfigs.getByName("release")
                                                                                    proguardFiles(
                                                                                                        getDefaultProguardFile("proguard-android-optimize.txt"),
                                                                                                                        "proguard-rules.pro"
                                                                                    )
                                }
                    }
                        compileOptions {
                                    sourceCompatibility = JavaVersion.VERSION_1_8
                                            targetCompatibility = JavaVersion.VERSION_1_8
                        }
                            kotlinOptions {
                                        jvmTarget = "1.8"
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
            implementation(libs.androidx.appcompat)
                implementation(libs.material)

                    // Jetpack Compose & ViewModel
                        implementation(platform(libs.androidx.compose.bom))
                            implementation(libs.androidx.compose.material3)
                                implementation(libs.androidx.compose.ui.tooling.preview)
                                    implementation(libs.androidx.lifecycle.viewmodel.compose)

                                        // Room DB
                                            implementation(libs.androidx.room.runtime)
                                                ksp(libs.androidx.room.compiler)
                                                    implementation(libs.androidx.room.ktx)

                                                        // DataStore
                                                            implementation(libs.androidx.datastore.preferences)

                                                                // ExoPlayer
                                                                    implementation("androidx.media3:media3-exoplayer:1.3.1")
                                                                        implementation("androidx.media3:media3-ui:1.3.1")
                                                                            implementation("androidx.compose.animation:animation")
                                                                                implementation("androidx.compose.animation:animation-core")
                                                                                    implementation("io.coil-kt:coil-compose:2.5.0")
                                                                                        implementation("io.coil-kt:coil-compose:2.7.0")
                                                                                            // Permissions
                                                                                                implementation("com.google.accompanist:accompanist-permissions:0.32.0")
                                                                                                    implementation("androidx.compose.material:material-icons-extended")
                                                                                                        implementation("io.coil-kt:coil-compose:2.5.0")
                                                                                                            implementation ("androidx.media:media:1.6.0")


                                                                                                                // Testing
                                                                                                                    testImplementation(libs.junit)
                                                                                                                        androidTestImplementation(libs.androidx.junit)
                                                                                                                            androidTestImplementation(libs.androidx.espresso.core)
                                                                                                                                implementation(libs.androidx.activity.compose)

}

fun gradleLocalProperties(projectRootDir: File): Properties {
        val properties = Properties()
            val localPropertiesFile = File(projectRootDir, "local.properties")
                if (localPropertiesFile.exists()) {
                            localPropertiesFile.inputStream().use { input ->
                                        properties.load(input)
                                                }
                }
                    return properties
}
