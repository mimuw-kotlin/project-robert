package com.github.br0b.katrix

import io.ktor.client.engine.*
import io.ktor.http.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.*
import net.folivo.trixnity.client.room.Timeline
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.*
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.rooms.DirectoryVisibility
import net.folivo.trixnity.core.model.EventId
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
                }).fold(onSuccess = { matrixClient ->
                matrixClient.startSync()
                Result.success(Client(matrixClient))
            }, onFailure = {
                Result.failure(it)
            })
        }

        private fun eventTransformer(eventFlow: Flow<TimelineEvent>): Flow<RoomMessage?> {
            return eventFlow.map { event ->
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
    }

    private val roomStateMap = mutableMapOf<RoomId, RoomState>()
    private val mutex = Mutex()

    fun getRooms() = matrixClient.room.getAll()

    suspend fun addRoom(name: String) = matrixClient.api.room.createRoom(
        name = name, visibility = DirectoryVisibility.PUBLIC
    )

    suspend fun leaveRoom(roomId: RoomId) = matrixClient.api.room.leaveRoom(roomId)

    suspend fun getRoomData(roomId: RoomId, nInitMessages: Long): Flow<RoomData> {
        mutex.withLock {
            return roomStateMap.getOrPut(roomId) {
                val timeline = getTimeline(roomId)
                val startFrom = getLatestEventId(roomId)

                timeline.init(
                    startFrom = startFrom,
                    configBefore = { this.maxSize = nInitMessages },
                )

                RoomState(
                    combine(
                        matrixClient.room.getById(roomId).map { it?.name?.explicitName },
                        getRoomUsers(roomId).flatten(),
                        timeline.state.map { it.elements }.flatten(),
                        timeline.state.map { it.canLoadAfter },
                    ) { name, users, messages, canLoadNewMessages ->
                        RoomData(
                            roomId = roomId,
                            name = name,
                            users = users,
                            messages = messages,
                            canLoadNewMessages = canLoadNewMessages,
                        )
                    },
                    timeline = timeline,
                    startFrom = startFrom,
                    nMessagesBefore = nInitMessages,
                )
            }.data
        }
    }

    suspend fun updateRoomData(roomId: RoomId) {
        mutex.withLock {
            roomStateMap[roomId]?.timeline?.loadAfter()
        }
    }

    suspend fun send(message: OutgoingMessage) = matrixClient.room.sendMessage(message.roomId) {
        text(message.body)
    }

    private suspend fun getRoomUsers(roomId: RoomId): Flow<Map<UserId, Flow<RoomUser?>>> {
        matrixClient.user.loadMembers(roomId)
        return matrixClient.user.getAll(roomId)
    }

    private suspend fun getLatestEventId(roomId: RoomId): EventId {
        return matrixClient.room.getLastTimelineEvent(roomId).filterNotNull().first().first().eventId
    }

    private fun getTimeline(roomId: RoomId): Timeline<Flow<RoomMessage?>> =
        matrixClient.room.getTimeline(roomId, ::eventTransformer)

    override fun close() {
        matrixClient.close()
    }

    data class LoginData(
        val baseUrl: Url,
        val identifier: IdentifierType.User,
        val password: String,
    )

    data class RoomState(
        val data: Flow<RoomData>,
        val timeline: Timeline<Flow<RoomMessage?>>,
        val startFrom: EventId,
        val nMessagesBefore: Long,
    )

    data class RoomData(
        val roomId: RoomId,
        val name: String?,
        val users: Map<UserId, RoomUser?>,
        val messages: List<RoomMessage>,
        val canLoadNewMessages: Boolean,
    )
}