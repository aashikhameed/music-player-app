plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.json:json:20231013")

    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    val jfxClassifier = when {
        osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm")) -> "mac-aarch64"
        osName.contains("mac") -> "mac"
        osName.contains("win") -> "win"
        else -> "linux"
    }
    implementation("org.openjfx:javafx-base:21.0.2:$jfxClassifier")
    implementation("org.openjfx:javafx-graphics:21.0.2:$jfxClassifier")
    implementation("org.openjfx:javafx-media:21.0.2:$jfxClassifier")
}

compose.desktop {
    application {
        mainClass = "com.aashik.music.desktop.MainKt"
        nativeDistributions {
            packageName = "MusicPlayer"
            packageVersion = "1.0.0"
        }
    }
}
