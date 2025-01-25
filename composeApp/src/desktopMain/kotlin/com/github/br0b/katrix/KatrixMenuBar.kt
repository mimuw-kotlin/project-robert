package com.github.br0b.katrix

import androidx.compose.runtime.*
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.github.br0b.katrix.dialogs.LoginDialog
import io.ktor.client.engine.*
import io.ktor.http.*

@Composable
fun FrameWindowScope.KatrixMenuBar(
    onLogin: (Client.LoginData) -> Unit,
    onLogout: () -> Unit,
    onDebug: () -> Unit,
    onQuit: () -> Unit,
) {
    var openLoginDialog by remember { mutableStateOf(false) }

    MenuBar {
        Menu("File") {
            Item("Log in", onClick = { openLoginDialog = true })
            Item("Log out", onClick = onLogout)
            Item("Debug", onClick = onDebug)
            Item("Quit", onClick = onQuit)
        }
    }

    if (openLoginDialog) {
        LoginDialog(
            onLogin = {
                onLogin(it)
                openLoginDialog = false
            },
            onDismissRequest = { openLoginDialog = false },
        )
    }
}
