package com.moongchijang.domain.groupbuy.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "공구 상세 조회자 heartbeat 요청")
data class GroupBuyViewerHeartbeatRequest(
    @field:Schema(
        description = "클라이언트가 생성/보관하는 조회 세션 식별자(UUID 권장)",
        example = "c89f364e-ef03-40f8-905f-f9a4de2d6d5e"
    )
    @field:NotBlank(message = "viewerSessionId는 필수입니다.")
    @field:Size(min = 8, max = 128, message = "viewerSessionId는 8~128자여야 합니다.")
    val viewerSessionId: String
)
