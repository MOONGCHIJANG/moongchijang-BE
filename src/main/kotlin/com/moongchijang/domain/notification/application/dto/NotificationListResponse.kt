package com.moongchijang.domain.notification.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 목록 조회 응답")
data class NotificationListResponse(

    @field:Schema(description = "알림 목록")
    val items: List<NotificationItemResponse>,

    @field:Schema(description = "다음 조회 커서 (마지막 알림 기준)", example = "2026-05-22T09:00:00|101")
    val nextCursor: String?,

    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean
)
