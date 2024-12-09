import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.github.goborori.katrix"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("net.folivo:trixnity-client:4.11.0")
    implementation("net.folivo:trixnity-client-repository-realm:4.11.0")
    implementation("net.folivo:trixnity-client-media-okio:4.11.0")
    implementation("org.slf4j:slf4j-api:2.0.9") // Core SLF4J API
    implementation("org.slf4j:slf4j-simple:2.0.9") // Simple implementation
    implementation("androidx.compose.runtime:runtime-desktop:1.7.0")
    implementation("io.ktor:ktor-client-core:3.0.2")
    implementation("io.ktor:ktor-client-okhttp:3.0.2")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Katrix"
            packageVersion = "1.0.0"
        }
    }
}
