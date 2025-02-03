package com.github.br0b.katrix.messages

import java.time.Instant

sealed class LogMessage {
    data class InfoMessage(
        val body: String,
        val timestamp: Instant,
    ) : LogMessage()

    data class ErrorMessage(
        val body: String,
        val timestamp: Instant,
    ) : LogMessage()
}
