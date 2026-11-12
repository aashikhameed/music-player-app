// The macOS app is its own build: it needs Kotlin 2.4 / Compose Multiplatform 1.12 for the Kyant backdrop
// (liquid glass) library, while the Android app stays on its own toolchain.
// Run it with: ./gradlew -p desktop run   (or :snapshot)
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
rootProject.name = "desktop"
