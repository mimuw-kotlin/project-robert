import net.folivo.trixnity.core.model.RoomId

data class Message (
    val text: String,
    val roomId: RoomId,
)