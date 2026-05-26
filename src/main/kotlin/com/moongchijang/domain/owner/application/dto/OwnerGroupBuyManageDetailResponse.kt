package com.moongchijang.domain.owner.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 공구 관리 상세 응답")
data class OwnerGroupBuyManageDetailResponse(
    @field:Schema(description = "공구 ID", example = "101")
    val groupBuyId: Long,

    @field:Schema(description = "공구 상태", example = "IN_PROGRESS")
    val status: OwnerGroupBuyManageFilterType,

    @field:Schema(description = "참여자 요약")
    val participantSummary: OwnerGroupBuyManageParticipantSummary,

    @field:Schema(description = "참여자 목록")
    val participants: List<OwnerGroupBuyParticipantItemResponse>
)
