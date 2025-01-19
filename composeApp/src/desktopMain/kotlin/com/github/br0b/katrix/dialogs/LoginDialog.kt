package com.github.br0b.katrix.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.br0b.katrix.Client
import com.github.br0b.katrix.FormField
import com.github.br0b.katrix.HiddenFormField
import io.ktor.http.*
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType

@Composable
fun LoginDialog(
    onLogin: (Client.LoginData) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var baseUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Login") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                FormField(baseUrl, "Homeserver", { baseUrl = it })
                FormField(username, "Username", { username = it })
                HiddenFormField(password, "Password") { password = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onLogin(Client.LoginData(Url(baseUrl), IdentifierType.User(username), password))
                },
                enabled = baseUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
            ) {
                Text("Login")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
