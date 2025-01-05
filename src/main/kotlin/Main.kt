import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.engine.okhttp.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun <T> TitleList(
    header: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    composable: @Composable (T) -> Unit
) {
    Column(modifier = modifier) {
        Text(header, style = MaterialTheme.typography.h6)
        LazyColumn {
            items(items) { item ->
                composable(item)
            }
        }
    }
}

@Composable
fun MessageInput() {
    var messageInput by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = messageInput,
            onValueChange = { messageInput = it }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { messageInput = "" },
        ) {
            Text("Send")
        }
    }
}

@Composable
fun RoomList(
    rooms: Flow<Set<Room>>?,
    chosenRoom: RoomId?,
    onRoomClick: (RoomId) -> Unit,
    modifier: Modifier = Modifier
) {
    val rooms_ by rooms?.collectAsState(initial = emptySet()) ?: mutableStateOf(emptySet())

    TitleList("Rooms", rooms_.toList(), modifier = modifier) {
        Text(
            it.name?.explicitName ?: it.roomId.toString(),
            modifier = Modifier
                .clickable(onClick = { onRoomClick(it.roomId) })
                .fillMaxWidth(),
            fontWeight = if (it.roomId == chosenRoom) FontWeight.Bold else  FontWeight.Normal)
    }
}

@Composable
fun MessageView(message: Message) {
    val timestamp = message.time.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    Text(
        text = "[$timestamp] ${message.text}",
        color = message.getColor(),
    )
}

@Composable
fun ChatScreen(viewModel: ChatViewModel = ChatViewModel()) {
    val state = viewModel.state.collectAsState()
    val activeRoomId = state.value.activeRoomId
    var roomUsersFlow by remember { mutableStateOf<Flow<List<RoomUser>>?>(null) }
    val roomUsers by roomUsersFlow?.collectAsState(emptyList()) ?: mutableStateOf(emptyList())

    LaunchedEffect(activeRoomId) {
        roomUsersFlow = activeRoomId?.let {
            state.value.client?.getRoomUsers(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoomList(
                    state.value.client?.getRooms()?.flattenValues(),
                    state.value.activeRoomId,
                    onRoomClick = {
                        if (activeRoomId != it)
                            viewModel.setActiveRoom(it)
                        else
                            viewModel.setActiveRoom(null)
                    },
                    modifier = Modifier.weight(1f)
                )
                TitleList(
                    "Messages",
                    state.value.mainMessages,
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxWidth()
                ) {
                    MessageView(it)
                }
                TitleList(
                    "Users",
                    roomUsers,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(it.name)
                }
            }
            Row {
                MessageInput()
            }
        }
    }
}

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
            Column(modifier = Modifier
                .fillMaxWidth()
            ) {
                FormField(baseUrl, "Homeserver", { baseUrl = it })
                FormField(username, "Username", { username = it })
                HiddenFormField(password, "Password") { password = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (baseUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                    onLogin(Client.LoginData(Url(baseUrl), IdentifierType.User(username), password))
                }
            }) {
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

@Composable
fun FormField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = value.isEmpty()
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = visualTransformation,
        isError = isError
    )
}

@Composable
fun HiddenFormField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    FormField(
        value = value,
        label = label,
        onValueChange = onValueChange,
        visualTransformation = PasswordVisualTransformation()
    )
}

@Composable
fun FrameWindowScope.KatrixMenuBar(
    onLogin: (Client.LoginData) -> Unit,
    onLogout: () -> Unit,
    onQuit: () -> Unit,
) {
    MenuBar {
        var openLoginDialog by remember { mutableStateOf(false) }

        Menu("File") {
            Item("Log in", onClick = { openLoginDialog = true })
            Item("Log out", onClick = onLogout)
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

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            val viewModel = ChatViewModel()
            val httpClientEngine = OkHttp.create()

            KatrixMenuBar(
                onLogin = { viewModel.login(it, httpClientEngine) },
                onLogout = { viewModel.logout() },
                onQuit = ::exitApplication
            )

            ChatScreen(viewModel)
        }
    }
}
