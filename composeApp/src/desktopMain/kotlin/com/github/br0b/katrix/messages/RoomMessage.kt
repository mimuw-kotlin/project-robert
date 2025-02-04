package com.github.br0b.katrix.messages

import com.github.br0b.katrix.ImageInfo
import net.folivo.trixnity.core.model.UserId
import java.time.Instant

data class RoomMessage(
    val body: String,
    val time: Instant,
    val senderId: UserId,
    val imageInfo: ImageInfo?
)
