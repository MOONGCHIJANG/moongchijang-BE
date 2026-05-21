package com.moongchijang.domain.participation.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "마이페이지 진행 중 탭 참여 카드 아이템")
data class InProgressParticipationItemResponse(

    @field:Schema(description = "참여 ID", example = "1201")
    val participationId: Long,

    @field:Schema(description = "공구 ID", example = "101")
    val groupBuyId: Long,

    @field:Schema(description = "상품명", example = "두쭌쿠 오리지널 1개")
    val productName: String,

    @field:Schema(description = "매장명", example = "사이드템포")
    val storeName: String,

    @field:Schema(description = "픽업 일시", example = "2026-04-15T14:00:00", nullable = true)
    val pickupAt: LocalDateTime?,

    @field:Schema(description = "결제 금액", example = "36000")
    val paidAmount: Int,

    @field:Schema(description = "수량", example = "2")
    val quantity: Int,

    @field:Schema(description = "달성률(%)", example = "72")
    val achievementRate: Int,

    @field:Schema(description = "마감 D-day 숫자", example = "2")
    val dDay: Int,

    @field:Schema(description = "참여 일시", example = "2026-04-12T10:30:00")
    val participatedAt: LocalDateTime
)
