package com.github.br0b.katrix

import io.ktor.client.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import java.time.Instant

class ChatViewModel(
    config: Config.() -> Unit = {},
) {
    private val config = Config().apply(config)

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val clientState = MutableStateFlow<ClientState?>(null)
    val clientData: Flow<ClientData?> = clientState.map { it?.data }

    fun login(loginData: Client.LoginData, httpClientEngine: HttpClientEngine) {
        scope.launch {
            pushMainMessages(LogMessage.InfoMessage("Logging in...", Instant.now()))
            Client.login(loginData, httpClientEngine).fold(
                onSuccess = { onLoginSuccess(it) },
                onFailure = { pushMainMessages(LogMessage.ErrorMessage(it.message ?: "Unknown error", Instant.now())) })
        }
    }

    fun logout() {
        pushMainMessages(LogMessage.InfoMessage("Logging out...", Instant.now()))
        supervisorJob.cancelChildren()
        clientState.update { null }
        pushMainMessages(LogMessage.InfoMessage("Logged out", Instant.now()))
    }

    fun send(message: OutgoingMessage) {
        scope.launch {
            clientState.value?.client?.send(message)
        }
    }

    fun addRoom(name: String) {
        clientState.value?.client?.let { client ->
            scope.launch {
                client.addRoom(name)
            }
        } ?: pushMainMessages(LogMessage.ErrorMessage("Log in to add rooms!", Instant.now()))
    }

    /**
     * If roomId is null, the active room is set to null.
     * Otherwise, the active room is set to the room with the given roomId.
     */
    fun setActiveRoom(roomId: RoomId?) {
        scope.launch {
            clientState.update {
                it?.let { currentClientState ->
                    val newActiveRoomData = roomId?.let { roomId ->
                        currentClientState.client.getRoomData(roomId, config.nInitialRoomMessages)
                    }
                    println("newActiveRoomData: $newActiveRoomData")
                    currentClientState.copy(
                        data = currentClientState.data.copy(
                            activeRoomData = newActiveRoomData
                        )
                    )
                }
            }
        }
    }

    fun fetchNewMessages(roomId: RoomId) {
        scope.launch {
            clientState.value?.client?.updateRoomData(roomId)
        }
    }

    private fun onLoginSuccess(client: Client) {
        pushMainMessages(LogMessage.InfoMessage("Logged in", Instant.now()))
        val rooms = client.getRooms().flattenValues().map {
            println("Fetching rooms...")
            it.toList()
        }
        clientState.update { ClientState(client, ClientData(rooms, null)) }
    }

    private fun pushMainMessages(message: LogMessage) {
        _state.update { currentState ->
            currentState.copy(mainMessages = currentState.mainMessages + message)
        }
    }

    fun leaveRoom(roomId: RoomId) {
        scope.launch {
            clientState.value?.client?.leaveRoom(roomId)
        }
    }

    data class UiState(
        val mainMessages: List<LogMessage> = emptyList(),
    )

    data class ClientState(
        val client: Client,
        val data: ClientData,
    )

    data class ClientData(
        val rooms: Flow<List<Room>>,
        val activeRoomData: Flow<Client.RoomData>?,
    )

    data class Config(
        val nInitialRoomMessages: Long = 10,
    )
}