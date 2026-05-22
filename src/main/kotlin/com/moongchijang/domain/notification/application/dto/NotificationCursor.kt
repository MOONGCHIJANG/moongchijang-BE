package com.moongchijang.domain.notification.application.dto

import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import java.time.LocalDateTime

data class NotificationCursor(
    val occurredAt: LocalDateTime,
    val id: Long
) {
    fun encode(): String = "$occurredAt|$id"

    companion object {
        fun decode(cursor: String): NotificationCursor {
            val parts = cursor.split("|", limit = 2)
            if (parts.size != 2) {
                throw CustomException(ErrorCode.INVALID_INPUT, "cursor format is invalid")
            }

            val occurredAt = runCatching { LocalDateTime.parse(parts[0]) }.getOrElse {
                throw CustomException(ErrorCode.INVALID_INPUT, "cursor occurredAt is invalid")
            }
            val id = parts[1].toLongOrNull()
                ?: throw CustomException(ErrorCode.INVALID_INPUT, "cursor id is invalid")

            return NotificationCursor(occurredAt = occurredAt, id = id)
        }
    }
}
