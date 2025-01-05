import androidx.compose.ui.graphics.Color
import net.folivo.trixnity.core.model.RoomId
import java.time.Instant

sealed class Message(
    val text: String,
    val time: Instant,
) {
    abstract fun getColor(): Color
}

class RoomMessage(
    text: String,
    time: Instant,
    private val roomId: RoomId,
) : Message(text, time) {
    fun getRoomId() = roomId

    override fun getColor(): Color = Color.Black
}

class InfoMessage(
    text: String,
    time: Instant
) : Message(text, time) {
    override fun getColor(): Color = Color.Blue
}

class ErrorMessage(
    text: String,
    time: Instant
) : Message(text, time) {
    override fun getColor(): Color = Color.Red
}
