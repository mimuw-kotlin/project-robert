package com.github.br0b.katrix.dialogs

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import com.github.br0b.katrix.FormField

@Composable
fun AddRoomDialog(
    onAddRoom: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var roomName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add room") },
        text = {
            FormField(roomName, "Room Name", { roomName = it })
        },
        confirmButton = {
            Button(onClick = { onAddRoom(roomName) }, enabled = roomName.isNotEmpty()) {
                Text("Add Room")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}
