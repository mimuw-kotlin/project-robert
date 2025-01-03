import net.folivo.trixnity.core.model.RoomId

data class ChatState (
    val client: Client? = null,
    val messages: List<Message> = emptyList(),
    val chosenRoom: RoomId? = null,
)