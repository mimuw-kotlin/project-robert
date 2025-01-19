package com.github.br0b.katrix

import io.ktor.client.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

class ChatViewModel {
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + supervisorJob)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _clientData = MutableStateFlow<ClientData?>(null)
    val clientData: StateFlow<ClientData?> = _clientData

    private var client = AtomicReference<Client?>(null)

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
        _state.update { it.copy(activeRoomId = null) }
        _clientData.update { null }
        pushMainMessages(LogMessage.InfoMessage("Logged out", Instant.now()))
    }

    fun send(message: OutgoingMessage) {
        scope.launch {
            client.get()?.send(message)
        }
    }

    fun addRoom(name: String) {
        client.get()?.let { client ->
            scope.launch {
                client.addRoom(name)
            }
        } ?: pushMainMessages(LogMessage.ErrorMessage("Log in to add rooms!", Instant.now()))
    }

    fun setActiveRoom(roomId: RoomId?) {
        _state.update { it.copy(activeRoomId = roomId) }

        scope.launch {
            client.get()?.let { client ->
                val activeRoomData = roomId?.let { client.getActiveRoomData(roomId) }

                _clientData.update { currentClientData ->
                    currentClientData?.copy(activeRoomData = activeRoomData) ?: ClientData(
                        client.getRooms(),
                        activeRoomData
                    )
                }
            } ?: _clientData.update { null }
        }
    }

    private fun onLoginSuccess(client: Client) {
        pushMainMessages(LogMessage.InfoMessage("Logged in", Instant.now()))
        this.client.set(client)
        _state.update { it.copy(activeRoomId = null) }
        scope.launch {
            _clientData.update { ClientData(client.getRooms(), null) }
        }
    }

    private fun pushMainMessages(message: LogMessage) {
        _state.update { currentState ->
            currentState.copy(mainMessages = currentState.mainMessages + message)
        }
    }

    fun leaveRoom(roomId: RoomId) {
        scope.launch {
            client.get()?.leaveRoom(roomId)
        }
    }

    data class UiState(
        val mainMessages: List<LogMessage> = emptyList(),
        val activeRoomId: RoomId? = null,
    )

    data class ClientData(
        val rooms: Flow<Map<RoomId, Flow<Room?>>>,
        val activeRoomData: Client.RoomData? = null,
    )
}