package com.github.br0b.katrix

import net.folivo.trixnity.core.model.RoomId

sealed class UiEvent

data class FetchRequested(val roomId: RoomId?) : UiEvent()

data class SendRequested(val message: OutgoingMessage) : UiEvent()