package com.moongchijang.domain.groupbuy.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공구 상세 활성 조회자 수 응답")
data class GroupBuyViewerCountResponse(

    @field:Schema(description = "현재 활성 조회자 수", example = "12")
    val activeViewerCount: Int,

    @field:Schema(
        description = "FOMO 문구/뱃지 노출 여부 (activeViewerCount >= threshold)",
        example = "true"
    )
    val showFomoBadge: Boolean,

    @field:Schema(description = "FOMO 노출 기준 인원 수", example = "10")
    val threshold: Int
)
