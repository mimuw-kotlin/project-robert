package com.github.br0b.katrix.messages

import net.folivo.trixnity.core.model.RoomId

data class OutgoingMessage(
    val roomId: RoomId,
    val body: String,
)
