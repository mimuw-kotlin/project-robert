import io.ktor.client.engine.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import net.folivo.trixnity.client.*
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId

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

    fun getRoom(roomId: RoomId): Flow<Room?> = matrixClient.room.getById(roomId)

    fun getRooms() = matrixClient.room.getAll()

    suspend fun getRoomUsers(roomId: RoomId): Flow<List<RoomUser>> {
        matrixClient.user.loadMembers(roomId)
        return matrixClient.user.getAll(roomId).flattenValues()
    }

    suspend fun send(message: RoomMessage) = matrixClient.room.sendMessage(message.getRoomId()) {
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