import io.ktor.client.engine.*
import io.ktor.http.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId

/**
 * A client that interacts with the Matrix server.
 */
class Client(
    private val httpClientEngine: HttpClientEngine
) : AutoCloseable {
    private val cache = Cache.create()

    /**
     * Created using the [login] method.
     */
    private var matrixClient: MatrixClient? = null

    suspend fun login(loginData: LoginData): Result<Unit> {
        val client = MatrixClient.login(
            baseUrl = loginData.baseUrl,
            identifier = loginData.identifier,
            password = loginData.password,
            repositoriesModule = cache.getRepositoryModule(),
            mediaStore = cache.getMediaStore(),
            configuration = getMatrixClientConfiguration(httpClientEngine)
        )

        matrixClient = client.getOrNull()
        matrixClient?.startSync()

        return client.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun logout() = matrixClient?.logout() ?: Result.success(Unit)

    fun getRooms() = matrixClient?.room?.getAll()

    suspend fun send(message: Message) = matrixClient?.room?.sendMessage(message.roomId) {
        text(message.text)
    }

    private fun getMatrixClientConfiguration(httpClientEngine: HttpClientEngine): MatrixClientConfiguration.() -> Unit = {
        this.httpClientEngine = httpClientEngine
    }

    override fun close() {
        matrixClient?.close()
    }

    data class LoginData(
        val baseUrl: Url,
        val identifier: IdentifierType.User,
        val password: String,
    )

    data class Message(
        val roomId: RoomId,
        val text: String,
    )
}