import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId

@Composable
fun <T> HeaderedList(
    header: @Composable () -> Unit,
    items: List<T>,
    modifier: Modifier = Modifier,
    isOrderReversed: Boolean = false,
    item: @Composable (T) -> Unit,
) {
    val scrollState = rememberLazyListState()

    LaunchedEffect (items) {
        if (isOrderReversed && items.isNotEmpty())
            scrollState.scrollToItem(items.size - 1)
    }

    Box(modifier = modifier) {
        Column {
            header()
            LazyColumn(
                state = scrollState,
                reverseLayout = isOrderReversed
            ) {
                items(items) {
                    item(it)
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

@Composable
fun MessageInput(activeRoomId: RoomId?, onSend: (OutgoingMessage) -> Unit) {
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
        activeRoomId?.let {
            Button(
                onClick = { onSend(OutgoingMessage(messageInput, activeRoomId)) },
                enabled = messageInput.isNotEmpty()
            ) {
                Text("Send")
            }
        } ?: Button(
            onClick = { /* Do nothing, because there is no active room. */ },
            enabled = false
        ) {
            Text("Send")
        }
    }
}

@Composable
fun RoomList(
    rooms: List<Room>,
    chosenRoom: RoomId?,
    onRoomClick: (RoomId) -> Unit,
    modifier: Modifier = Modifier
) {
    HeaderedList(
        header = { Text("Rooms") },
        rooms,
        modifier = modifier
    ) {
        Text(
            it.name?.explicitName ?: it.roomId.toString(),
            modifier = Modifier
                .clickable(onClick = { onRoomClick(it.roomId) })
                .fillMaxWidth(),
            fontWeight = if (it.roomId == chosenRoom) FontWeight.Bold else  FontWeight.Normal
        )
    }
}

@Composable
fun MessageView(body: String, color: Color) {
    Text(
        text = body,
        color = color,
    )
}

@Composable
fun MainMessages(messages: List<LogMessage>, modifier: Modifier = Modifier) {
    HeaderedList(
        header = { Text("Main", textDecoration = TextDecoration.Underline) },
        items = messages,
        modifier = modifier,
    ) {
        MessageView(it.getFormatted(), it.getColor())
    }
}

@Composable
fun RoomMessages(room: String, messages: List<Flow<RoomMessage?>>, modifier: Modifier = Modifier) {
    HeaderedList(
        header = { Text(room) },
        items = messages,
        modifier = modifier,
        isOrderReversed = true,
    ) { messageFlow ->
        messageFlow.collectAsState(null).value?.let { message ->
            MessageView(message.getFormatted(), message.getColor())
        }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel = ChatViewModel()) {
    val state = viewModel.state.collectAsState().value
    val activeRoomId = state.activeRoom?.roomId

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
                    state.rooms.values.toList(),
                    activeRoomId,
                    onRoomClick = {
                        if (it != activeRoomId)
                            viewModel.setActiveRoom(it)
                        else
                            viewModel.setActiveRoom(null)
                    },
                    modifier = Modifier.weight(1f)
                )

                val messagesModifier = Modifier.weight(3f)
                state.activeRoom?.let { activeRoom ->
                    RoomMessages(viewModel.getRoomName(activeRoom.roomId), activeRoom.messages, messagesModifier)
                } ?: MainMessages(state.mainMessages, messagesModifier)

                HeaderedList(
                    { Text("Users") },
                    state.activeRoom?.users ?: emptyList(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(it.name)
                }
            }
            MessageInput(activeRoomId) { viewModel.send(it) }
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

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            val viewModel = ChatViewModel()
            val httpClientEngine = OkHttp.create()

            KatrixMenuBar(
                onLogin = { viewModel.login(it, httpClientEngine) },
                onLogout = { viewModel.logout() },
                onDebug = { debugLogin(viewModel, httpClientEngine) },
                onQuit = ::exitApplication
            )
            ChatScreen(viewModel)
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
