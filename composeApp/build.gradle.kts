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
        val desktopTest by getting

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
            implementation(libs.kotlinx.collections.immutable)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        // Adds the desktop test dependency
        desktopTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

ktlint {
    version = "0.47.1"

    dependencies {
        ktlintRuleset(libs.ktlint.compose.rules)
    }

    filter {
        exclude { element ->
            val path = element.file.path
            path.contains("/build/")
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
