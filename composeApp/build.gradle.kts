import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ktlint)
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
            implementation(libs.trixnity.client)
            implementation(libs.trixnity.client.repository.realm)
            implementation(libs.trixnity.client.media.okio)
            implementation(libs.slf4j.api)
            implementation(libs.slf4j.simple)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
        }

        commonTest.dependencies {
            implementation(libs.ui.test.junit4)
            implementation(libs.ui.test.manifest)
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
