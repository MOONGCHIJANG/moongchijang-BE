package com.moongchijang.domain.notification.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 기간 섹션")
enum class NotificationSection {
    @Schema(description = "오늘")
    TODAY,
    @Schema(description = "어제")
    YESTERDAY,
    @Schema(description = "이전")
    OLDER
}
