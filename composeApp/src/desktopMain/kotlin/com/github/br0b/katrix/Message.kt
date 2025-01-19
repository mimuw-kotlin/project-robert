package com.github.br0b.katrix

import androidx.compose.ui.graphics.Color
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import java.time.Instant

sealed class LogMessage(
    body: String,
    time: Instant,
) {
    private val formatted = "[$time]: $body"

    fun getFormatted(): String {
        return formatted
    }

    abstract fun getColor(): Color

    class InfoMessage(
        text: String,
        time: Instant
    ) : LogMessage(text, time) {
        override fun getColor(): Color = Color.Blue
    }

    class ErrorMessage(
        text: String,
        time: Instant
    ) : LogMessage(text, time) {
        override fun getColor(): Color = Color.Red
    }
}

class RoomMessage(
    private val body: String,
    private val time: Instant,
    val senderId: UserId,
) {
    fun getFormatted(sender: String) = "[$time] $sender: $body"

    fun getColor(): Color = Color.Black
}

data class OutgoingMessage(
    val body: String,
    val roomId: RoomId
)
