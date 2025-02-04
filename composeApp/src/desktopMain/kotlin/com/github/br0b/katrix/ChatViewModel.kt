package com.github.br0b.katrix

import com.github.br0b.katrix.messages.LogMessage
import com.github.br0b.katrix.messages.OutgoingMessage
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    config: Config.() -> Unit = {}
) {
    data class Config(
        val nMessageBatchSize: Long = 10
    )

    private val _scope = CoroutineScope(Dispatchers.IO)
    private val _config = Config().apply(config)

    private val _client = MutableStateFlow<Client?>(null)
    private val _activeRoomFlow = MutableStateFlow<Flow<Client.RoomState>?>(null)

    private val _mainMessages = MutableStateFlow<List<LogMessage>>(emptyList())
    val mainMessages: StateFlow<List<LogMessage>> = _mainMessages

    /**
     * Username of the logged-in user.
     */
    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username

    private val _activeRoomId = MutableStateFlow<RoomId?>(null)
    val activeRoomId: StateFlow<RoomId?> = _activeRoomId

    private val _activeRoomState = MutableStateFlow<Client.RoomState?>(null)
    val activeRoomState: StateFlow<Client.RoomState?> = _activeRoomState

    private val _roomsState = MutableStateFlow(emptyList<Room>())
    val roomsState: StateFlow<List<Room>> = _roomsState

    init {
        // Update active room state.
        _scope.launch {
            _activeRoomFlow.flatMapLatest {
                it ?: flowOf(null)
            }.collect { room ->
                _activeRoomState.update { room }
            }
        }

        // Update the list of rooms.
        _scope.launch {
            _client.flatMapLatest { it?.getRooms() ?: flowOf(emptyList()) }.collect { rooms ->
                _roomsState.update { rooms }
            }
        }
    }

    fun login(
        loginData: Client.LoginData,
        httpClientEngine: HttpClientEngine
    ) {
        _scope.launch {
            if (_client.value != null) {
                pushMainMessages(LogMessage.ErrorMessage("Already logged in", Instant.now()))
            } else {
                pushMainMessages(LogMessage.InfoMessage("Logging in...", Instant.now()))
                Client.login(loginData, httpClientEngine).fold(
                    onSuccess = {
                        pushMainMessages(
                            LogMessage.InfoMessage(
                                "Logged in",
                                Instant.now()
                            )
                        )
                        onLoginSuccess(it)
                    },
                    onFailure = {
                        pushMainMessages(
                            LogMessage.ErrorMessage(
                                it.message ?: "Unknown error",
                                Instant.now()
                            )
                        )
                    }
                )
            }
        }
    }

    fun logout() {
        pushMainMessages(LogMessage.InfoMessage("Logging out...", Instant.now()))
        _client.value?.close()
        _client.update { null }
        _username.update { null }
        _activeRoomFlow.update { null }
        _roomsState.update { emptyList() }
        pushMainMessages(LogMessage.InfoMessage("Logged out", Instant.now()))
    }

    /**
     * If roomId is null, the active room is set to null.
     * Otherwise, the active room is set to the room with the given roomId.
     */
    fun setActiveRoom(roomId: RoomId?) {
        val newRoomId = if (_activeRoomId.value == roomId) null else roomId

        _activeRoomId.update { newRoomId }

        _scope.launch {
            _activeRoomFlow.update {
                newRoomId?.let { roomId ->
                    _client.value?.getRoomFlow(roomId, _config.nMessageBatchSize)
                }
            }
        }
    }

    /**
     * Create a new room on the server.
     */
    fun createRoom(name: String) {
        _scope.launch {
            _client.value?.createRoom(name) ?: pushMainMessages(
                LogMessage.ErrorMessage(
                    "Log in to add rooms!",
                    Instant.now()
                )
            )
        }
    }

    /**
     * Leave a room on the server.
     */
    fun leaveRoom(roomId: RoomId) {
        _scope.launch {
            _client.value?.leaveRoom(roomId)
        }

        if (_activeRoomId.value == roomId) {
            setActiveRoom(null)
        }
    }

    /**
     * Send a message to the matrix server.
     */
    fun send(message: OutgoingMessage) {
        _scope.launch {
            _client.value?.send(message)
        }
    }

    fun loadOldMessages() {
        _scope.launch {
            _activeRoomId.value?.let { roomId ->
                _client.value?.loadOldMessages(roomId, _config.nMessageBatchSize)
            }
        }
    }

    fun loadNewMessages() {
        _scope.launch {
            _activeRoomId.value?.let { roomId ->
                _client.value?.loadNewMessages(roomId)
            }
        }
    }

    suspend fun loadThumbnail(info: ImageInfo) = _client.value?.loadThumbnail(info)

    private fun onLoginSuccess(client: Client) {
        _client.update { client }
        _username.update { client.getUserId().toString() }
    }

    private fun pushMainMessages(message: LogMessage) {
        _mainMessages.update { it + message }
    }
}
