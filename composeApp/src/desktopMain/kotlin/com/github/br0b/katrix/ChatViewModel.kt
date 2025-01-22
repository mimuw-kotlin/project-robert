package com.github.br0b.katrix

import io.ktor.client.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import java.time.Instant

class ChatViewModel(
    config: Config.() -> Unit = {},
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val config = Config().apply(config)
    private val roomStateMap = mutableMapOf<RoomId, Flow<Client.RoomState>>()

    private var client: Client? = null
    private var roomsStateJob: Job? = null
    private var activeRoomStateJob: Job? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _activeRoomState = MutableStateFlow<Client.RoomState?>(null)
    val activeRoomState: StateFlow<Client.RoomState?> = _activeRoomState

    private val _roomsState = MutableStateFlow(emptyList<Room>())
    val roomsState: StateFlow<List<Room>> = _roomsState

    fun login(loginData: Client.LoginData, httpClientEngine: HttpClientEngine) {
        scope.launch {
            if (client != null) {
                pushMainMessages(LogMessage.ErrorMessage("Already logged in", Instant.now()))
            } else {
                pushMainMessages(LogMessage.InfoMessage("Logging in...", Instant.now()))
                Client.login(loginData, httpClientEngine).fold(onSuccess = { onLoginSuccess(it) }, onFailure = {
                    pushMainMessages(
                        LogMessage.ErrorMessage(
                            it.message ?: "Unknown error", Instant.now()
                        )
                    )
                })
            }
        }
    }

    fun logout() {
        pushMainMessages(LogMessage.InfoMessage("Logging out...", Instant.now()))
        scope.launch {
            roomsStateJob?.cancel()
            roomsStateJob?.join()
            activeRoomStateJob?.cancel()
            activeRoomStateJob?.join()
            client?.close()
            client = null
            roomStateMap.clear()
            _uiState.update { it.copy(activeRoomId = null) }
            _activeRoomState.update { null }
            _roomsState.update { emptyList() }
            pushMainMessages(LogMessage.InfoMessage("Logged out", Instant.now()))
        }
    }

    /**
     * If roomId is null, the active room is set to null.
     * Otherwise, the active room is set to the room with the given roomId.
     */
    fun setActiveRoom(roomId: RoomId?) {
        scope.launch {
            val newRoomId = if (_uiState.value.activeRoomId == roomId) null else roomId

            val activeRoomFlow = newRoomId?.let { roomId ->
                client?.getRoomFlow(roomId, config.nMessageBatchSize)
            }

            _uiState.update { currentUiState ->
                currentUiState.copy(activeRoomId = newRoomId)
            }

            activeRoomStateJob?.cancel()
            activeRoomStateJob?.join()
            println("Previous active room state job cancelled")
            activeRoomStateJob = scope.launch {
                activeRoomFlow?.let { flow ->
                    _activeRoomState.update { Client.RoomState() }
                    println("[activeRoomState]: Collecting new room state from $activeRoomFlow")
                    flow.collect { newActiveRoomState ->
                        _activeRoomState.update {
                            println("[activeRoomState]: Collected new room state: $newActiveRoomState")
                            newActiveRoomState
                        }
                    }
                } ?: _activeRoomState.update { null }
            }
        }
    }

    fun addRoom(name: String) {
        scope.launch {
            client?.addRoom(name) ?: pushMainMessages(
                LogMessage.ErrorMessage(
                    "Log in to add rooms!",
                    Instant.now()
                )
            )
        }
    }

    fun leaveRoom(roomId: RoomId) {
        scope.launch {
            client?.leaveRoom(roomId)
        }
    }

    fun send(message: OutgoingMessage) {
        scope.launch {
            client?.send(message)
        }
    }

    fun fetchOldMessages() {
        scope.launch {
            _uiState.value.activeRoomId?.let { roomId ->
                client?.loadOldMessages(roomId, config.nMessageBatchSize)
            }
        }
    }

    fun fetchNewMessages() {
        scope.launch {
            _uiState.value.activeRoomId?.let { roomId ->
                client?.loadNewMessages(roomId)
            }
        }
    }

    private fun onLoginSuccess(client: Client) {
        pushMainMessages(LogMessage.InfoMessage("Logged in", Instant.now()))
        this.client = client
        roomsStateJob = scope.launch {
            client.getRooms().collect { newRooms ->
                _roomsState.update { newRooms }
            }
        }
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