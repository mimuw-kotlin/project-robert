package com.github.br0b.katrix

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.ktor.client.engine.HttpClientEngine
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.jetbrains.skia.Image

@Composable
fun App(
    viewModel: ChatViewModel,
    clientEngine: HttpClientEngine,
    modifier: Modifier = Modifier
) {
    MaterialTheme {
        ChatScreen(viewModel, clientEngine, modifier = modifier)
    }
}

/**
 * The main screen of the chat application.
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    clientEngine: HttpClientEngine,
    modifier: Modifier = Modifier
) {
    val activeRoomId by viewModel.activeRoomId.collectAsState()

    Box(
        modifier = modifier
    ) {
        Column {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Rooms(
                    viewModel.roomsState,
                    activeRoomId,
                    onRoomClick = { viewModel.setActiveRoom(it) },
                    onAddRoom = { name -> viewModel.createRoom(name) },
                    onLeaveRoom = { viewModel.leaveRoom(it) }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserStatus(
                    usernameFlow = viewModel.username,
                    onLogin = { viewModel.login(it, clientEngine) },
                    onLogout = { viewModel.logout() }
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
    modifier: Modifier = Modifier
) {
    val rooms by roomsState.collectAsState()
    var isAdditionDialogOpen by remember { mutableStateOf(false) }
    var roomToLeave by remember { mutableStateOf<RoomId?>(null) }

    Column(modifier = modifier) {
        TextWithButton({ Text("Rooms") }, "+", onClick = { isAdditionDialogOpen = true })
        ScrollableList(
            rooms.filter { it.membership == Membership.JOIN }.toImmutableList()
        ) { room ->
            val id = room.roomId
            val roomName = room.name?.explicitName ?: id.toString()

            TextWithButton(
                {
                    Text(
                        roomName,
                        modifier = Modifier.clickable(onClick = { onRoomClick(id) }),
                        fontWeight = if (id == activeRoom) FontWeight.Bold else FontWeight.Normal
                    )
                },
                "-",
                onClick = { roomToLeave = id }
            )
        }
    }

    if (isAdditionDialogOpen) {
        AddRoomDialog(
            onAddRoom = {
                onAddRoom(it)
                isAdditionDialogOpen = false
            },
            onDismissRequest = { isAdditionDialogOpen = false }
        )
    }

    roomToLeave?.let {
        ConfirmDialog(
            onConfirm = {
                onLeaveRoom(it)
                roomToLeave = null
            },
            onDismissRequest = { roomToLeave = null }
        )
    }
}

@Composable
fun RoomMessages(
    roomStateFlow: StateFlow<Client.RoomState?>,
    onLoadOldMessages: () -> Unit,
    onLoadNewMessages: () -> Unit,
    onLoadThumbnail: suspend (ImageInfo) -> Result<Flow<ByteArray>>?,
    modifier: Modifier = Modifier
) {
    roomStateFlow.collectAsState().value?.let { roomState ->
        if (roomState.canLoadNewMessages) {
            onLoadNewMessages()
        }

        WithHeader(
            header = { Text(roomState.name ?: "Loading...") },
            modifier = modifier
        ) {
            Column {
                if (roomState.canLoadOldMessages) {
                    Button(onClick = onLoadOldMessages) {
                        Text("Load more timeline events")
                    }
                }
                ScrollableList(
                    items = roomState.messages.reversed().toImmutableList(),
                    isOrderReversed = true,
                    modifier = Modifier.fillMaxWidth()
                ) { message ->
                    val senderId = message.senderId
                    val sender = roomState.users[senderId]?.name ?: senderId.toString()
                    Column {
                        Text("[${message.time}] $sender: ${message.body}", color = Color.Black)
                        message.imageInfo?.let { imgInfo ->
                            var byteArray by remember { mutableStateOf<ByteArray?>(null) }

                            LaunchedEffect(imgInfo) {
                                onLoadThumbnail(imgInfo)?.fold(
                                    onSuccess = { byteArrayFlow ->
                                        byteArrayFlow.collect { byteArray = it }
                                    },
                                    onFailure = { println("Failed to load image: $it") }
                                )
                            }

                            byteArray?.let {
                                val bitmap = Image.makeFromEncoded(it).toComposeImageBitmap()
                                Image(bitmap, imgInfo.name)
                            }
                        }
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
    modifier: Modifier = Modifier
) {
    WithHeader(
        header = { Text("Main", textDecoration = TextDecoration.Underline) },
        modifier
    ) {
        ScrollableList(messages.collectAsState().value.toImmutableList()) { msg ->
            when (msg) {
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
    modifier: Modifier = Modifier
) {
    val users = roomState.collectAsState().value?.users ?: emptyMap()

    Column(modifier) {
        Text("Users")
        ScrollableList(users.entries.toImmutableList()) { (id, user) -> Text(user?.name ?: id.toString()) }
    }
}

@Composable
fun UserStatus(
    usernameFlow: StateFlow<String?>,
    onLogin: (Client.LoginData) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLoginDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
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
    }

    if (showLoginDialog) {
        LoginDialog(
            onLogin = {
                onLogin(it)
                showLoginDialog = false
            },
            onDismissRequest = { showLoginDialog = false }
        )
    }
}

/**
 * A text field for sending a message to a Matrix room.
 */
@Composable
fun MessageInput(
    activeRoomId: RoomId?,
    modifier: Modifier = Modifier,
    onSend: (OutgoingMessage) -> Unit
) {
    var messageInput by remember { mutableStateOf("") }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = messageInput,
            onValueChange = { messageInput = it }
        )
        Spacer(modifier = Modifier.width(8.dp))
        activeRoomId?.let {
            Button(
                onClick = {
                    onSend(OutgoingMessage(activeRoomId, messageInput))
                    messageInput = ""
                },
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
fun <T> ScrollableList(
    items: ImmutableList<T>,
    modifier: Modifier = Modifier,
    isOrderReversed: Boolean = false,
    item: @Composable (T) -> Unit
) {
    val scrollState = rememberLazyListState()

    Box(modifier = modifier) {
        Column {
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
            adapter = rememberScrollbarAdapter(scrollState),
            reverseLayout = isOrderReversed
        )
    }
}

@Composable
fun WithHeader(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    block: @Composable () -> Unit
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        text()
        Surface(
            modifier =
            Modifier.width(24.dp).clickable(onClick = onClick)
                .align(Alignment.CenterVertically)
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
