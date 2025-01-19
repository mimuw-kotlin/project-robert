import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation("net.folivo:trixnity-client:4.11.0")
            implementation("net.folivo:trixnity-client-repository-realm:4.11.0")
            implementation("net.folivo:trixnity-client-media-okio:4.11.0")
            implementation("org.slf4j:slf4j-api:2.0.9") // Core SLF4J API
            implementation("org.slf4j:slf4j-simple:2.0.9") // Simple implementation
            implementation("io.ktor:ktor-client-core:3.0.2")
            implementation("io.ktor:ktor-client-okhttp:3.0.2")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.github.br0b.katrix.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.github.br0b.katrix"
            packageVersion = "1.0.0"
        }
    }
}
