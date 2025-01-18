package com.github.br0b.katrix

import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId

data class ChatState (
    val mainMessages: List<LogMessage> = emptyList(),
    val rooms: Map<RoomId, Room> = emptyMap(),
    val activeRoom: RoomData? = null,
)