package com.moongchijang.domain.notification.application.dto

import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.domain.entity.NotificationType
import com.moongchijang.domain.user.domain.entity.UserRole
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
    REQUEST,
    @Schema(description = "오늘 픽업 알림")
    TODAY_PICKUP,
    @Schema(description = "리마인더 알림")
    REMINDER,
    @Schema(description = "확정 알림")
    CONFIRMED,
    @Schema(description = "취소 알림")
    CANCELLED;

    fun toNotificationTypeOrNull(): NotificationType? {
        return when (this) {
            ALL -> null
            WISH -> NotificationType.WISH
            APPLY -> NotificationType.APPLY
            PICKUP -> NotificationType.PICKUP
            REQUEST -> NotificationType.REQUEST
            TODAY_PICKUP, REMINDER, CONFIRMED, CANCELLED -> null
        }
    }

    fun ownerTriggerTypesOrNull(): Set<NotificationTriggerType>? {
        return when (this) {
            TODAY_PICKUP -> setOf(NotificationTriggerType.OWNER_PICKUP_SAME_DAY_MORNING)
            REMINDER -> setOf(NotificationTriggerType.OWNER_PICKUP_DAY_BEFORE_MORNING)
            CONFIRMED -> setOf(
                NotificationTriggerType.OWNER_GROUPBUY_ACHIEVED_IMMEDIATE,
                NotificationTriggerType.OWNER_OPEN_REQUEST_APPROVED_IMMEDIATE,
                NotificationTriggerType.OWNER_ORDER_CONFIRM_REQUIRED_IMMEDIATE
            )
            CANCELLED -> setOf(
                NotificationTriggerType.OWNER_GROUPBUY_FAILED_IMMEDIATE,
                NotificationTriggerType.OWNER_ORDER_CANCELLED_IMMEDIATE,
                NotificationTriggerType.OWNER_OPEN_REQUEST_REJECTED_IMMEDIATE,
                NotificationTriggerType.OWNER_CLOSE_REQUEST_REJECTED_IMMEDIATE
            )
            ALL, WISH, APPLY, PICKUP, REQUEST -> null
        }
    }

    fun isOwnerFilter(): Boolean = ownerTriggerTypesOrNull() != null

    fun supportsRole(role: UserRole): Boolean {
        return when (role) {
            UserRole.BUYER -> this in setOf(ALL, WISH, APPLY, PICKUP, REQUEST)
            UserRole.SELLER -> this in setOf(ALL, TODAY_PICKUP, REMINDER, CONFIRMED, CANCELLED)
            UserRole.ADMIN -> false
        }
    }

    companion object {
        fun from(value: String): NotificationCategory {
            return entries.firstOrNull { it.name == value.uppercase() }
                ?: throw CustomException(
                    ErrorCode.INVALID_INPUT,
                    "category must be one of ALL, WISH, APPLY, PICKUP, REQUEST, TODAY_PICKUP, REMINDER, CONFIRMED, CANCELLED"
                )
        }
    }
}
