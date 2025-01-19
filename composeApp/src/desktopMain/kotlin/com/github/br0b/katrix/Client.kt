package com.github.br0b.katrix

import io.ktor.client.engine.*
import io.ktor.http.*
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.*
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.room.toFlowList
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.rooms.DirectoryVisibility
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import java.time.Instant

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
                onSuccess = { matrixClient ->
                    matrixClient.startSync()
                    Result.success(Client(matrixClient))
                },
                onFailure = {
                    Result.failure(it)
                }
            )
        }
    }

    fun getRooms() = matrixClient.room.getAll()

    suspend fun addRoom(name: String) = matrixClient.api.room.createRoom(
        name = name,
        visibility = DirectoryVisibility.PUBLIC
    )

    suspend fun leaveRoom(roomId: RoomId) = matrixClient.api.room.leaveRoom(roomId)

    suspend fun getActiveRoomData(roomId: RoomId): RoomData {
        return RoomData(
            roomId = roomId,
            name = matrixClient.room.getById(roomId).map { it?.name?.explicitName },
            users = getRoomUsers(roomId),
            messages = getMessages(roomId, MutableStateFlow(10)),
        )
    }

    private suspend fun getRoomUsers(roomId: RoomId): Flow<Map<UserId, Flow<RoomUser?>>> {
        matrixClient.user.loadMembers(roomId)
        return matrixClient.user.getAll(roomId)
    }

    suspend fun send(message: OutgoingMessage) = matrixClient.room.sendMessage(message.roomId) {
        text(message.body)
    }

    private fun getMessages(roomId: RoomId, maxSize: StateFlow<Int>): Flow<List<Flow<RoomMessage?>>> {
        return matrixClient.room.getLastTimelineEvents(roomId)
            .toFlowList(maxSize)
            .map { events ->
                events.map { eventFlow ->
                    eventFlow
                        .map { event ->
                            event.content?.getOrNull()?.let { content ->
                                if (content is RoomMessageEventContent) {
                                    RoomMessage(
                                        body = content.body,
                                        time = Instant.ofEpochMilli(event.originTimestamp),
                                        senderId = event.sender
                                    )
                                } else {
                                    null
                                }
                            }
                        }
                }
            }.filterNotNull()
    }

    override fun close() {
        matrixClient.close()
    }

    data class LoginData(
        val baseUrl: Url,
        val identifier: IdentifierType.User,
        val password: String,
    )

    data class RoomData(
        val roomId: RoomId,
        val name: Flow<String?>,
        val users: Flow<Map<UserId, Flow<RoomUser?>>>,
        val messages: Flow<List<Flow<RoomMessage?>>>,
    )
}