import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.folivo.trixnity.core.model.RoomId

@Composable
fun TitleList(
    header: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(header, style = MaterialTheme.typography.h6)
        LazyColumn(
            contentPadding = PaddingValues(8.dp)
        ) {
            items(items) { item ->
                Text(item)
            }
        }
    }
}

@Composable
fun UserInput() {
    var userInput by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = userInput,
            onValueChange = { userInput = it }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { userInput = "" },
        ) {
            Text("Send")
        }
    }
}

@Composable
fun ChatScreen() {
    var rooms by remember { mutableStateOf(listOf(Room("Room 1"), Room("Room 2"))) }
    var users by remember { mutableStateOf(listOf(User("User 1"), User("User 2"))) }
    // List of 100 messages
    var messages by remember { mutableStateOf(List(100) { Message("Message $it", RoomId("TEST")) }) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ){
        Row {
            TitleList("Rooms", rooms.map { it.name })
            Column(
                Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                TitleList(
                    "Messages",
                    messages.map { it.text },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                UserInput()
            }
            TitleList("Users", users.map { it.name })
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            ChatScreen()
        }
    }
}
