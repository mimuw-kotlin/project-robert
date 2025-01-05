import io.ktor.client.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.core.model.RoomId
import java.time.Instant

class ChatViewModel : CoroutineScope {
    private val job = Job()
    override val coroutineContext = Dispatchers.Default + job
    private var loginJob: Job? = null

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    fun login(loginData: Client.LoginData, httpClientEngine: HttpClientEngine) {
        pushMessage(InfoMessage("Logging in...", Instant.now()))
        loginJob?.cancel()
        loginJob = launch {
            try {
                Client.login(loginData, httpClientEngine).fold(
                    onSuccess = { client ->
                        pushMessage(InfoMessage("Logged in", Instant.now()))
                        _state.update { currentState ->
                            currentState.client?.close()
                            currentState.copy(client = client)
                        }
                    },
                    onFailure = {
                        pushMessage(ErrorMessage(it.message ?: "Unknown error", Instant.now()))
                    }
                )
            } catch (e: CancellationException) {
                pushMessage(InfoMessage("Logged out", Instant.now()))
            }
        }
    }

    fun setActiveRoom(roomId: RoomId?) {
        _state.update { currentState ->
            currentState.copy(activeRoomId = roomId)
        }
    }

    fun logout() {
        pushMessage(InfoMessage("Logging out...", Instant.now()))
        loginJob?.cancel()
        _state.update { currentState ->
            currentState.client?.close()
            currentState.copy(client = null)
        }
    }

    private fun pushMessage(message: Message) {
        _state.update { currentState ->
            currentState.copy(mainMessages = currentState.mainMessages + message)
        }
    }
}