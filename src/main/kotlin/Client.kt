import io.ktor.client.engine.*
import io.ktor.http.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.room
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

    suspend fun logout() = matrixClient.logout()

    fun getRooms() = matrixClient.room.getAll()

    suspend fun send(message: Message) = matrixClient.room.sendMessage(message.roomId) {
        text(message.text)
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