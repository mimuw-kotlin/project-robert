package com.github.br0b.katrix.dialogs

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDialog(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Are you sure?") },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismissRequest
            ) {
                Text("No")
            }
        }
    )
}
