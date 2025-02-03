package com.github.br0b.katrix

import com.github.br0b.katrix.messages.OutgoingMessage
import com.github.br0b.katrix.messages.RoomMessage
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
class Client private constructor(
    private val matrixClient: MatrixClient,
) : AutoCloseable {
    companion object {
        suspend fun login(
            loginData: LoginData,
            httpClientEngine: HttpClientEngine,
        ): Result<Client> {
            val cache = Cache.create()

            return MatrixClient.login(
                baseUrl = loginData.baseUrl,
                identifier = loginData.identifier,
                password = loginData.password,
                repositoriesModule = cache.getRepositoryModule(),
                mediaStore = cache.getMediaStore(),
                configuration = {
                    this.httpClientEngine = httpClientEngine
                },
            ).fold(onSuccess = { matrixClient ->
                matrixClient.startSync()
                Result.success(Client(matrixClient))
            }, onFailure = {
                Result.failure(it)
            })
        }

        /**
         * An event transformer for creating a Timeline of RoomMessages.
         */
        private fun eventTransformer(eventFlow: Flow<TimelineEvent>): Flow<RoomMessage?> {
            return eventFlow.map { event ->
                event.content?.getOrNull()?.let { content ->
                    when (content) {
                        is RoomMessageEventContent.TextBased -> RoomMessage(
                            body = content.body,
                            time = Instant.ofEpochMilli(event.originTimestamp),
                            senderId = event.sender,
                            thumbnailInfo = null,
                        )
                        is RoomMessageEventContent.FileBased.Image ->RoomMessage(
                            body = content.fileName?.let { fileName -> if (content.body != fileName) content.body else null} ?: content.body,
                            time = Instant.ofEpochMilli(event.originTimestamp),
                            senderId = event.sender,
                            thumbnailInfo = content.info?.let { info ->
                                content.url?.let { url ->
                                    info.width?.let { width ->
                                        info.height?.let { height ->
                                            ImageInfo(
                                                name = content.fileName ?: content.body,
                                                url = url,
                                                width = width,
                                                height = height
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        else -> null
                    }
                }
            }
        }
    }

    data class LoginData(
        val baseUrl: Url,
        val identifier: IdentifierType.User,
        val password: String,
    )

    data class RoomState(
        val id: RoomId,
        val name: String?,
        val users: Map<UserId, RoomUser?>,
        val messages: List<RoomMessage>,
        val canLoadOldMessages: Boolean,
        val canLoadNewMessages: Boolean,
    )

    /**
     * When we get a timeline of a room from the server, we store it here.
     */
    private val timelineMap = mutableMapOf<RoomId, Timeline<Flow<RoomMessage?>>>()
    private val timelineMutex = Mutex()

    fun getRooms() = matrixClient.room.getAll().flattenValues().map(Set<Room>::toList)

    suspend fun createRoom(name: String) =
        matrixClient.api.room.createRoom(
            name = name,
            visibility = DirectoryVisibility.PUBLIC,
        )

    suspend fun leaveRoom(roomId: RoomId) = matrixClient.api.room.leaveRoom(roomId)

    /**
     * Get the statae of a room.
     *
     * @param nInitEvents The number of initial events we want to fetch.
     */
    suspend fun getRoomFlow(
        roomId: RoomId,
        nInitEvents: Long,
    ): Flow<RoomState> {
        timelineMutex.withLock {
            val timeline =
                timelineMap.getOrPut(roomId) {
                    getTimeline(roomId).apply {
                        val startFrom = getLatestEventId(roomId)

                        init(
                            startFrom = startFrom,
                            configBefore = { this.maxSize = nInitEvents },
                        )
                    }
                }

            return combine(
                matrixClient.room.getById(roomId).map { it?.name?.explicitName },
                getRoomUsers(roomId).flatten(),
                timeline.state.map { it.elements }.flatten(),
                timeline.state.map { it.canLoadBefore },
                timeline.state.map { it.canLoadAfter },
            ) { name, users, messages, canLoadOldMessages, canLoadNewMessages ->
                RoomState(
                    roomId,
                    name,
                    users,
                    messages,
                    canLoadOldMessages,
                    canLoadNewMessages,
                )
            }
        }
    }

    suspend fun loadOldMessages(
        roomId: RoomId,
        maxNMessages: Long,
    ) {
        timelineMutex.withLock {
            val change = timelineMap[roomId]?.loadBefore { this.maxSize = maxNMessages }
            println("Number of new elements: ${change?.newElements?.size ?: 0}")
        }
    }

    suspend fun loadNewMessages(roomId: RoomId) {
        timelineMutex.withLock {
            timelineMap[roomId]?.loadAfter()
        }
    }

    suspend fun loadThumbnail(imageInfo: ImageInfo): Result<Flow<ByteArray>> {
        println("Loading image $imageInfo...")
        return matrixClient.media.getThumbnail(imageInfo.url, 10 * imageInfo.width.toLong(), 10 * imageInfo.height.toLong())
    }

    suspend fun send(message: OutgoingMessage) =
        matrixClient.room.sendMessage(message.roomId) {
            text(message.body)
        }

    fun getUserId() = matrixClient.userId

    override fun close() {
        matrixClient.close()
    }

    private suspend fun getRoomUsers(roomId: RoomId): Flow<Map<UserId, Flow<RoomUser?>>> {
        matrixClient.user.loadMembers(roomId)
        return matrixClient.user.getAll(roomId)
    }

    private suspend fun getLatestEventId(roomId: RoomId): EventId {
        return matrixClient.room.getLastTimelineEvent(roomId).filterNotNull().first().first().eventId
    }
    private fun getTimeline(roomId: RoomId): Timeline<Flow<RoomMessage?>> = matrixClient.room.getTimeline(roomId, ::eventTransformer)
}
