import androidx.compose.ui.graphics.Color
import net.folivo.trixnity.core.model.RoomId

sealed class Message(private val text: String) {
    fun getText() = text

    abstract fun getColor(): Color
}

class RoomMessage(
    private val text: String,
    private val roomId: RoomId,
) : Message(text) {
    fun getRoomId() = roomId

    override fun getColor(): Color = Color.Black
}

class InfoMessage(
    private val text: String,
) : Message(text) {
    override fun getColor(): Color = Color.Blue
}

class ErrorMessage(
    private val text: String,
) : Message(text) {
    override fun getColor(): Color = Color.Red
}