package com.moongchijang.domain.notification.application.dto

import com.moongchijang.domain.notification.application.deeplink.NotificationDeeplinkSchema
import com.moongchijang.domain.notification.domain.entity.Notification
import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType
import com.moongchijang.domain.notification.domain.entity.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "알림 항목")
data class NotificationItemResponse(

    @field:Schema(description = "알림 ID", example = "101")
    val id: Long,

    @field:Schema(description = "알림 종류", example = "PICKUP")
    val type: NotificationType,

    @field:Schema(description = "알림 제목", example = "픽업 리마인드")
    val title: String,

    @field:Schema(description = "알림 본문 요약", example = "오늘 오전 9시에 픽업이 시작됩니다.")
    val body: String,

    @field:Schema(description = "읽음 여부", example = "false")
    val isRead: Boolean,

    @field:Schema(description = "알림 발생 시각", example = "2026-05-22T09:00:00")
    val occurredAt: LocalDateTime,

    @field:Schema(description = "연결 대상 ID", example = "2001")
    val targetId: Long?,

    @field:Schema(description = "딥링크 타입", example = "PICKUP_GUIDE")
    val deeplinkType: NotificationDeeplinkType,

    @field:Schema(
        description = "딥링크 파라미터. PICKUP_GUIDE는 participationId, GROUPBUY_DETAIL/MY_APPLYING은 groupBuyId, REQUEST_STATUS는 requestId를 사용합니다.",
        example = "{\"participationId\":\"2001\"}"
    )
    val deeplinkParams: Map<String, String>,

    @field:Schema(description = "기간 구분", example = "TODAY")
    val section: NotificationSection
) {
    companion object {
        fun from(notification: Notification, today: LocalDate): NotificationItemResponse {
            return NotificationItemResponse(
                id = notification.id,
                type = notification.type,
                title = notification.title,
                body = notification.body,
                isRead = notification.isRead,
                occurredAt = notification.occurredAt,
                targetId = notification.targetId,
                deeplinkType = notification.deeplinkType,
                deeplinkParams = NotificationDeeplinkSchema.toParams(notification.deeplinkType, notification.targetId),
                section = resolveSection(notification.occurredAt.toLocalDate(), today)
            )
        }

        private fun resolveSection(date: LocalDate, today: LocalDate): NotificationSection {
            return when (date) {
                today -> NotificationSection.TODAY
                today.minusDays(1) -> NotificationSection.YESTERDAY
                else -> NotificationSection.OLDER
            }
        }
    }
}
