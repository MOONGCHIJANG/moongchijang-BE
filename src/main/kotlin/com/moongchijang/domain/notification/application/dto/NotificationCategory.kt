package com.moongchijang.domain.notification.application.dto

import com.moongchijang.domain.notification.domain.entity.NotificationType
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 카테고리 필터")
enum class NotificationCategory {
    @Schema(description = "전체 알림")
    ALL,
    @Schema(description = "찜 알림")
    WISH,
    @Schema(description = "신청 알림")
    APPLY,
    @Schema(description = "픽업 알림")
    PICKUP,
    @Schema(description = "요청 알림")
    REQUEST;

    fun toNotificationTypeOrNull(): NotificationType? {
        return when (this) {
            ALL -> null
            WISH -> NotificationType.WISH
            APPLY -> NotificationType.APPLY
            PICKUP -> NotificationType.PICKUP
            REQUEST -> NotificationType.REQUEST
        }
    }

    companion object {
        fun from(value: String): NotificationCategory {
            return entries.firstOrNull { it.name == value.uppercase() }
                ?: throw CustomException(ErrorCode.INVALID_INPUT, "category must be one of ALL, WISH, APPLY, PICKUP, REQUEST")
        }
    }
}
