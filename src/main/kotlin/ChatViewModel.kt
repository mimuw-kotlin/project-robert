import io.ktor.client.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.core.model.RoomId
import java.time.Instant

class ChatViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    private val uiEvents: SharedFlow<UiEvent> = _uiEvents

    private val _maxTimelineSize = MutableStateFlow(20)
    private val maxTimelineSize: StateFlow<Int> = _maxTimelineSize

    private var isClientRunning = false

    fun login(loginData: Client.LoginData, httpClientEngine: HttpClientEngine) {
        pushMainMessages(InfoMessage("Logging in...", Instant.now()))
        scope.launch {
            Client.login(loginData, httpClientEngine).fold(
                onSuccess = { client ->
                    pushMainMessages(InfoMessage("Logged in", Instant.now()))
                    isClientRunning = true
                    launchClientJob(client)
                },
                onFailure = {
                    pushMainMessages(ErrorMessage(it.message ?: "Unknown error", Instant.now()))
                }
            )
        }
    }

    fun send(message: OutgoingMessage) {
        println("Sending message: $message")
        scope.launch {
            _uiEvents.emit(SendRequested(message))
        }
    }

    fun setActiveRoom(roomId: RoomId?) {
        scope.launch {
            println("Setting active room to $roomId")
            _uiEvents.emit(FetchRequested(roomId))
        }
    }

    fun getRoomName(roomId: RoomId): String {
        return state.value.rooms[roomId]?.name?.explicitName ?: roomId.toString()
    }

    fun logout() {
        pushMainMessages(InfoMessage("Logging out...", Instant.now()))
        scope.cancel()
        _state.update { currentState ->
            currentState.copy(
                rooms = emptyMap(),
                activeRoom = null,
            )
        }
        pushMainMessages(InfoMessage("Logged out", Instant.now()))
        isClientRunning = false
    }

    private fun launchClientJob(client: Client) {
        scope.launch {
            launch {
                fetchRooms(client)
            }
            launch {
                fetchActiveRoomData(client)
            }
        }
    }

    private suspend fun fetchRooms(client: Client) {
        val roomsFlow = client.getRooms().flatten()
        roomsFlow.collect { rooms ->
            println("Rooms fetched")
            _state.update { currentState ->
                currentState.copy(rooms = rooms.filter { it.value != null }.mapValues { it.value!! })
            }
        }
    }

    private suspend fun fetchActiveRoomUsers(client: Client, roomId: RoomId) {
        client.getRoomUsers(roomId).collect { users ->
            _state.update { currentState ->
                currentState.copy(activeRoom = currentState.activeRoom?.copy(users = users))
            }
        }
    }

    private suspend fun fetchActiveRoomMessages(client: Client, roomId: RoomId) {
        client.getMessages(roomId, maxTimelineSize).collect { message ->
            _state.update { currentState ->
                currentState.copy(activeRoom = currentState.activeRoom?.copy(messages = message))}
        }
    }

    private suspend fun fetchActiveRoomData(client: Client) {
        var activeRoomJob: Job? = null

        uiEvents.collect { event ->
            println("UI event: $event")
            when (event) {
                is FetchRequested -> {
                    activeRoomJob?.cancel()
                    event.roomId?.let { roomId ->
                        _state.update { currentState ->
                            currentState.copy(activeRoom = RoomData(roomId, emptyList(), emptyList()))
                        }
                        activeRoomJob = scope.launch {
                            launch {
                                fetchActiveRoomUsers(client, roomId)
                            }
                            launch {
                                fetchActiveRoomMessages(client, roomId)
                            }
                        }
                    } ?: run {
                        _state.update { currentState ->
                            currentState.copy(activeRoom = null)
                        }
                    }
                }
                is SendRequested -> {
                    client.send(event.message)
                }
            }
        }
    }

    private fun pushMainMessages(message: LogMessage) {
        _state.update { currentState ->
            currentState.copy(mainMessages = currentState.mainMessages + message)
        }
    }
}