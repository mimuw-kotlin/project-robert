package com.github.br0b.katrix

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.github.br0b.katrix.dialogs.AddRoomDialog
import com.github.br0b.katrix.dialogs.ConfirmDialog
import com.github.br0b.katrix.dialogs.LoginDialog
import com.github.br0b.katrix.messages.LogMessage
import com.github.br0b.katrix.messages.OutgoingMessage
import io.ktor.client.engine.*
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.jetbrains.skia.Image

@Composable
fun App(
    viewModel: ChatViewModel,
    clientEngine: HttpClientEngine,
) {
    MaterialTheme {
        ChatScreen(viewModel, clientEngine)
    }
}

/**
 * The main screen of the chat application.
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    clientEngine: HttpClientEngine,
) {
    val activeRoomId by viewModel.activeRoomId.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(8.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Rooms(
                    viewModel.roomsState,
                    activeRoomId,
                    onRoomClick = { viewModel.setActiveRoom(it) },
                    onAddRoom = { name -> viewModel.createRoom(name) },
                    onLeaveRoom = { viewModel.leaveRoom(it) },
                )

                Box(modifier = Modifier.weight(3f)) {
                    if (activeRoomId != null) {
                        RoomMessages(
                            viewModel.activeRoomState,
                            onLoadOldMessages = { viewModel.loadOldMessages() },
                            onLoadNewMessages = { viewModel.loadNewMessages() },
                            onLoadThumbnail = { viewModel.loadThumbnail(it) }
                        )
                    } else {
                        MainMessages(viewModel.mainMessages)
                    }
                }

                Users(viewModel.activeRoomState, Modifier.weight(1f))
            }
            Row {
                UserStatus(
                    usernameFlow = viewModel.username,
                    onLogin = { viewModel.login(it, clientEngine) },
                    onLogout = { viewModel.logout() },
                )
                MessageInput(activeRoomId) { viewModel.send(it) }
            }
        }
    }
}

/**
 * A list of rooms the current user is a member of.
 */
@Composable
fun Rooms(
    roomsState: StateFlow<List<Room>>,
    activeRoom: RoomId?,
    onRoomClick: (RoomId) -> Unit,
    onAddRoom: (String) -> Unit,
    onLeaveRoom: (RoomId) -> Unit,
) {
    val rooms by roomsState.collectAsState()
    var isAdditionDialogOpen by remember { mutableStateOf(false) }
    var roomToLeave by remember { mutableStateOf<RoomId?>(null) }

    Column {
        TextWithButton({ Text("Rooms") }, "+", onClick = { isAdditionDialogOpen = true })
        ScrollableList(
            rooms.filter { it.membership == Membership.JOIN },
        ) { room ->
            val id = room.roomId
            val roomName = room.name?.explicitName ?: id.toString()

            TextWithButton(
                {
                    Text(
                        roomName,
                        modifier = Modifier.clickable(onClick = { onRoomClick(id) }),
                        fontWeight = if (id == activeRoom) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                "-",
                onClick = { roomToLeave = id },
            )
        }
    }

    if (isAdditionDialogOpen) {
        AddRoomDialog(
            onAddRoom = {
                onAddRoom(it)
                isAdditionDialogOpen = false
            },
            onDismissRequest = { isAdditionDialogOpen = false },
        )
    }

    roomToLeave?.let {
        ConfirmDialog(
            onConfirm = {
                onLeaveRoom(it)
                roomToLeave = null
            },
            onDismissRequest = { roomToLeave = null },
        )
    }
}

@Composable
fun RoomMessages(
    roomStateFlow: StateFlow<Client.RoomState?>,
    onLoadOldMessages: () -> Unit,
    onLoadNewMessages: () -> Unit,
    onLoadThumbnail: (ImageInfo) -> StateFlow<Result<ByteArray>?>,
    modifier: Modifier = Modifier,
) {
    roomStateFlow.collectAsState().value?.let { roomState ->
        if (roomState.canLoadNewMessages) {
            onLoadNewMessages()
        }

        WithHeader(
            header = { Text(roomState.name ?: "Loading...") },
            modifier = modifier,
        ) {
            Column {
                if (roomState.canLoadOldMessages) {
                    Button(onClick = onLoadOldMessages) {
                        Text("Load more timeline events")
                    }
                }
                ScrollableList(
                    items = roomState.messages.reversed(),
                    isOrderReversed = true,
                    modifier = Modifier.fillMaxWidth(),
                ) { message ->
                    val senderId = message.senderId
                    val sender = roomState.users[senderId]?.name ?: senderId.toString()
                    Column {
                        Text("[${message.time}] $sender: ${message.body}", color = Color.Blue)
                        message.thumbnailInfo?.let { imgInfo ->
                            println("[App] Loading thumbnail for ${imgInfo.name}...")
                            onLoadThumbnail(imgInfo).collectAsState().value?.fold(
                                onSuccess = { byteArray ->
                                    println("Byte array size: ${byteArray.size}")
                                    val bitmap = Image.makeFromEncoded(byteArray).toComposeImageBitmap()
                                    Image(bitmap, imgInfo.name)
                                },
                                onFailure = { Text("Couldn't load ${imgInfo.name}") }
                            ) ?: Text(imgInfo.name)
                        } ?: println("No thumbnail info")
                    }
                }
            }
        }
    }
}

/**
 * A list of log messages.
 */
@Composable
fun MainMessages(
    messages: StateFlow<List<LogMessage>>,
    modifier: Modifier = Modifier,
) {
    WithHeader(
        header = { Text("Main", textDecoration = TextDecoration.Underline) },
        modifier,
    ) {
        ScrollableList(messages.collectAsState().value) { msg ->
            when(msg) {
                is LogMessage.InfoMessage -> {
                    Text("[${msg.timestamp}]: ${msg.body}", color = Color.Blue)
                }
                is LogMessage.ErrorMessage -> {
                    Text("[${msg.timestamp}]: ${msg.body}", color = Color.Red)
                }
            }
        }
    }
}

/**
 * A list of users in the active room.
 */
@Composable
fun Users(
    roomState: StateFlow<Client.RoomState?>,
    modifier: Modifier = Modifier,
) {
    val users = roomState.collectAsState().value?.users ?: emptyMap()

    Column(modifier) {
        Text("Users")
        ScrollableList(users.entries.toList()) { (id, user) -> Text(user?.name ?: id.toString()) }
    }
}

@Composable
fun UserStatus(
    usernameFlow: StateFlow<String?>,
    onLogin: (Client.LoginData) -> Unit,
    onLogout: () -> Unit,
) {
    var showLoginDialog by remember { mutableStateOf(false) }

    usernameFlow.collectAsState().value?.let { username ->
        Surface {
            Column {
                Text(username)
                Button(onClick = onLogout, modifier = Modifier.testTag("LOGOUT_BUTTON")) {
                    Text("Logout")
                }
            }
        }
    } ?: Button(onClick = { showLoginDialog = true }, modifier = Modifier.testTag("LOGIN_BUTTON")) {
        Text("Login")
    }

    if (showLoginDialog) {
        LoginDialog(
            onLogin = {
                onLogin(it)
                showLoginDialog = false
            },
            onDismissRequest = { showLoginDialog = false },
        )
    }
}

/**
 * A text field for sending a message to a Matrix room.
 */
@Composable
fun MessageInput(
    activeRoomId: RoomId?,
    onSend: (OutgoingMessage) -> Unit,
) {
    var messageInput by remember { mutableStateOf("") }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = messageInput,
            onValueChange = { messageInput = it },
        )
        Spacer(modifier = Modifier.width(8.dp))
        activeRoomId?.let {
            Button(
                onClick = {
                    onSend(OutgoingMessage(activeRoomId, messageInput))
                    messageInput = ""
                },
                enabled = messageInput.isNotEmpty(),
            ) {
                Text("Send")
            }
        } ?: Button(
            onClick = { /* Do nothing, because there is no active room. */ },
            enabled = false,
        ) {
            Text("Send")
        }
    }
}

@Composable
fun <T> ScrollableList(
    items: List<T>,
    modifier: Modifier = Modifier,
    isOrderReversed: Boolean = false,
    item: @Composable (T) -> Unit,
) {
    val scrollState = rememberLazyListState()

    Box(modifier = modifier) {
        Column {
            LazyColumn(
                state = scrollState,
                reverseLayout = isOrderReversed,
            ) {
                items(items) {
                    item(it)
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState),
            reverseLayout = isOrderReversed,
        )
    }
}

@Composable
fun WithHeader(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    block: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        header()
        block()
    }
}

/**
 * A text with a button next to it.
 */
@Composable
fun TextWithButton(
    text: @Composable () -> Unit,
    buttonText: String,
    onClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        text()
        Surface(
            modifier =
                Modifier.width(24.dp).clickable(onClick = onClick)
                    .align(Alignment.CenterVertically),
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
