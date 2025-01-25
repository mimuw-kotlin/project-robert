package com.github.br0b.katrix

import io.ktor.client.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    config: Config.() -> Unit = {},
) {
    private val _scope = CoroutineScope(Dispatchers.IO)
    private val _config = Config().apply(config)

    // Used for communication with jobs.
    private val _client = MutableStateFlow<Client?>(null)
    private val _activeRoomFlow = MutableStateFlow<Flow<Client.RoomState>?>(null)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username

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

        // Update rooms state.
        _scope.launch {
            _client.flatMapLatest { it?.getRooms() ?: flowOf(emptyList()) }.collect { rooms ->
                _roomsState.update { rooms }
            }
        }
    }

    fun login(
        loginData: Client.LoginData,
        httpClientEngine: HttpClientEngine,
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
                                Instant.now(),
                            ),
                        )
                        onLoginSuccess(it)
                    },
                    onFailure = {
                        pushMainMessages(
                            LogMessage.ErrorMessage(
                                it.message ?: "Unknown error",
                                Instant.now(),
                            ),
                        )
                    },
                )
            }
        }
    }

    fun logout() {
        pushMainMessages(LogMessage.InfoMessage("Logging out...", Instant.now()))
        _client.value?.close()
        _client.update { null }
        _uiState.update { it.copy(activeRoomId = null) }
        _username.update { null }
        _activeRoomState.update { null }
        _roomsState.update { emptyList() }
        pushMainMessages(LogMessage.InfoMessage("Logged out", Instant.now()))
    }

    /**
     * If roomId is null, the active room is set to null.
     * Otherwise, the active room is set to the room with the given roomId.
     */
    fun setActiveRoom(roomId: RoomId?) {
        val newRoomId = if (_uiState.value.activeRoomId == roomId) null else roomId

        _uiState.update { currentUiState ->
            currentUiState.copy(activeRoomId = newRoomId)
        }

        _scope.launch {
            _activeRoomFlow.update {
                newRoomId?.let { roomId ->
                    _client.value?.getRoomFlow(roomId, _config.nMessageBatchSize)
                }
            }
        }
    }

    fun addRoom(name: String) {
        _scope.launch {
            _client.value?.addRoom(name) ?: pushMainMessages(
                LogMessage.ErrorMessage(
                    "Log in to add rooms!",
                    Instant.now(),
                ),
            )
        }
    }

    fun leaveRoom(roomId: RoomId) {
        _scope.launch {
            _client.value?.leaveRoom(roomId)
        }

        if (_uiState.value.activeRoomId == roomId) {
            setActiveRoom(null)
        }
    }

    fun send(message: OutgoingMessage) {
        _scope.launch {
            _client.value?.send(message)
        }
    }

    fun fetchOldMessages() {
        _scope.launch {
            _uiState.value.activeRoomId?.let { roomId ->
                _client.value?.loadOldMessages(roomId, _config.nMessageBatchSize)
            }
        }
    }

    fun fetchNewMessages() {
        _scope.launch {
            _uiState.value.activeRoomId?.let { roomId ->
                _client.value?.loadNewMessages(roomId)
            }
        }
    }

    private fun onLoginSuccess(client: Client) {
        _client.update { client }
        _username.update { client.getUserId().toString() }
    }

    private fun pushMainMessages(message: LogMessage) {
        _uiState.update { currentState ->
            currentState.copy(mainMessages = currentState.mainMessages + message)
        }
    }

    data class UiState(
        val mainMessages: List<LogMessage> = emptyList(),
        val activeRoomId: RoomId? = null,
    )

    data class Config(
        val nMessageBatchSize: Long = 10,
    )
}
