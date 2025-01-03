import io.ktor.client.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ChatViewModel : CoroutineScope {
    private val job = Job()
    override val coroutineContext = Dispatchers.Default + job
    private var loginJob: Job? = null

    private val _state = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _state

    fun login(loginData: Client.LoginData, httpClientEngine: HttpClientEngine) {
        pushMessage(InfoMessage("Logging in..."))
        loginJob?.cancel()
        loginJob = launch {
            try {
                Client.login(loginData, httpClientEngine).fold(
                    onSuccess = { client ->
                        pushMessage(InfoMessage("Logged in"))
                        _state.update { currentState ->
                            currentState.client?.close()
                            currentState.copy(client = client)
                        }
                    },
                    onFailure = {
                        pushMessage(ErrorMessage(it.message ?: "Unknown error"))
                    }
                )
            } catch (e: CancellationException) {
                pushMessage(InfoMessage("Logged out"))
            }
        }

    }

    fun logout() {
        pushMessage(InfoMessage("Logging out..."))
        loginJob?.cancel()
        _state.update { currentState ->
            currentState.client?.close()
            currentState.copy(client = null)
        }
    }

    private fun pushMessage(message: Message) {
        _state.update { currentState ->
            currentState.copy(messages = currentState.messages + message)
        }
    }
}