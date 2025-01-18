package com.github.br0b.katrix

import kotlinx.coroutines.flow.Flow
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.core.model.RoomId

data class RoomData(
    val roomId: RoomId,
    val users: List<RoomUser>,
    val messages: List<Flow<RoomMessage?>>,
)
