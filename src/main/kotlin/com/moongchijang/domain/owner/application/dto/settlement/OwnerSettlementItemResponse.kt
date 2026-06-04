package com.moongchijang.domain.owner.application.dto.settlement

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "사장님 정산 공구 카드 응답")
data class OwnerSettlementItemResponse(

    @field:Schema(description = "공구 ID", example = "901001")
    val groupBuyId: Long,

    @field:Schema(description = "공구명", example = "딸기 타르트")
    val productName: String,

    @field:Schema(description = "참여 인원 수", example = "18")
    val participantCount: Int,

    @field:Schema(description = "픽업일", example = "2026-05-02")
    val pickupDate: LocalDate,

    @field:Schema(description = "정산 금액", example = "432000")
    val amount: Long,

    @field:Schema(description = "정산 상태", example = "SETTLEMENT_COMPLETED")
    val settlementStatus: OwnerSettlementStatus,
)
