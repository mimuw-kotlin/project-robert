package com.github.br0b.katrix

import androidx.compose.runtime.*
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import io.ktor.client.engine.*
import io.ktor.http.*
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType

@Composable
fun FrameWindowScope.KatrixMenuBar(
    onLogin: (Client.LoginData) -> Unit,
    onLogout: () -> Unit,
    onDebug: () -> Unit,
    onQuit: () -> Unit,
) {
    MenuBar {
        var openLoginDialog by remember { mutableStateOf(false) }

        Menu("File") {
            Item("Log in", onClick = { openLoginDialog = true })
            Item("Log out", onClick = onLogout)
            Item("Debug", onClick = onDebug)
            Item("Quit", onClick = onQuit)
        }

        if (openLoginDialog) {
            LoginDialog(
                onLogin = {
                    openLoginDialog = false
                    onLogin(it)
                },
                onDismissRequest = { openLoginDialog = false }
            )
        }
    }
}

fun debugLogin(viewModel: ChatViewModel, httpClientEngine: HttpClientEngine) {
    viewModel.login(
        Client.LoginData(
            Url("http://localhost:8008"),
            IdentifierType.User("aha"),
            "@3%(BzJtj|LxA\\U-<N*N"
        ),
        httpClientEngine
    )
}
