package com.github.br0b.katrix

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.http.*
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Katrix"
        ) {
            val viewModel = ChatViewModel()
            val clientEngine = OkHttp.create()

            App(viewModel, clientEngine, Modifier.fillMaxSize().padding(8.dp))
            KatrixMenuBar(
                onLogin = { viewModel.login(it, clientEngine) },
                onLogout = { viewModel.logout() },
                onDebug = { debugLogin(viewModel, clientEngine) },
                onQuit = ::exitApplication
            )
        }
    }

/**
 * Logs in with a hardcoded user and password.
 */
fun debugLogin(
    viewModel: ChatViewModel,
    httpClientEngine: HttpClientEngine
) {
    viewModel.login(
        Client.LoginData(
            Url("http://localhost:8008"),
            IdentifierType.User("aha"),
            "@3%(BzJtj|LxA\\U-<N*N"
        ),
        httpClientEngine
    )
}
