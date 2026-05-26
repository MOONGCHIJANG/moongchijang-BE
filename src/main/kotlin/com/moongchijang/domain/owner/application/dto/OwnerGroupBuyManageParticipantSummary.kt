package com.moongchijang.domain.owner.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 공구 관리 상세 상단 요약")
data class OwnerGroupBuyManageParticipantSummary(
    @field:Schema(description = "총 참여자 수", example = "20")
    val totalCount: Int,

    @field:Schema(description = "완료 인원 수", example = "8")
    val completedCount: Int,

    @field:Schema(description = "대기 중 인원 수", example = "12")
    val waitingCount: Int
)
