import io.ktor.client.engine.*
import io.ktor.http.*
import net.folivo.trixnity.client.*
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType

/**
 * A client that interacts with the Matrix server.
 */
class Client(
    private val matrixClient: MatrixClient,
) : AutoCloseable {

    companion object {
        suspend fun login(loginData: LoginData, httpClientEngine: HttpClientEngine): Result<Client> {
            val cache = Cache.create()

            return MatrixClient.login(
                baseUrl = loginData.baseUrl,
                identifier = loginData.identifier,
                password = loginData.password,
                repositoriesModule = cache.getRepositoryModule(),
                mediaStore = cache.getMediaStore(),
                configuration = {
                    this.httpClientEngine = httpClientEngine
                }
            ).fold(
                onSuccess = { client ->
                    client.startSync()
                    Result.success(Client(client))},
                onFailure = {
                    Result.failure(it)
                }
            )
        }
    }

    fun getStatus() = matrixClient.loginState

    fun getRooms() = matrixClient.room.getAll()

    suspend fun send(message: RoomMessage) = matrixClient.room.sendMessage(message.getRoomId()) {
        text(message.getText())
    }

    override fun close() {
        matrixClient.close()
    }

    data class LoginData(
        val baseUrl: Url,
        val identifier: IdentifierType.User,
        val password: String,
    )
}