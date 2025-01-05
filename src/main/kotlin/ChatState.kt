import net.folivo.trixnity.core.model.RoomId

data class ChatState (
    val client: Client? = null,
    val mainMessages: List<Message> = emptyList(),
    val activeRoomId: RoomId? = null,
)