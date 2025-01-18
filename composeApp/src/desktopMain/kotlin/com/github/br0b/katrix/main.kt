package com.github.br0b.katrix

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Katrix",
    ) {
        val viewModel = ChatViewModel()
        val clientEngine = OkHttp.create()

        App(viewModel)
        KatrixMenuBar(
            onLogin = { viewModel.login(it, clientEngine) },
            onLogout = { viewModel.logout() },
            onDebug = { debugLogin(viewModel, clientEngine) },
            onQuit = ::exitApplication,
        )
    }
}