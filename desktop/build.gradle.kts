plugins {
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("org.jetbrains.compose") version "1.12.0"
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")
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

// The Kyant backdrop library is built for Java 21+, so the app runs on a JDK 21+ (Gradle finds one under ~/.gradle/jdks or
// the installed JDKs; set -Pdesktop.jdk=/path/to/jdk to override).
val runtimeJdk: String? = (project.findProperty("desktop.jdk") as String?)
    ?: file(System.getProperty("user.home") + "/.gradle/jdks").listFiles()
        ?.filter { it.name.contains("-2") && (it.name.contains("-21-") || it.name.contains("-24-") || it.name.contains("-25-")) }
        ?.mapNotNull { dir -> dir.walkTopDown().firstOrNull { it.name == "Home" && it.resolve("bin/java").exists() }?.absolutePath }
        ?.firstOrNull()

compose.desktop {
    application {
        runtimeJdk?.let { javaHome = it }
        mainClass = "com.aashik.music.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
            packageName = "Music"
            packageVersion = "1.0.0"
            macOS { iconFile.set(project.file("icons/icon.icns")) }
        }
    }
}

// Dev helper: ./gradlew :desktop:snapshot -Pout=/tmp/x.png  (renders the UI off-screen, no display needed)
tasks.register<JavaExec>("snapshot") {
    group = "verification"
    mainClass.set("com.aashik.music.desktop.tools.SnapshotKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf((project.findProperty("out") as String?) ?: "/tmp/desktop.png", "1280", "720", (project.findProperty("mode") as String?) ?: "library")
    jvmArgs("-Djava.awt.headless=true")
    runtimeJdk?.let { executable = "$it/bin/java" }
}

