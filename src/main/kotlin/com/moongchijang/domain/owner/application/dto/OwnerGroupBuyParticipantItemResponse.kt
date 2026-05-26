package com.moongchijang.domain.owner.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(description = "사장님 공구 상세 참여자 정보")
data class OwnerGroupBuyParticipantItemResponse(
    @field:Schema(description = "참여자 이름", example = "홍길동")
    val name: String,

    @field:Schema(description = "참여자 전화번호", example = "01012345678")
    val phoneNumber: String,

    @field:Schema(description = "상품명", example = "두쫀쿠 세트")
    val productName: String,

    @field:Schema(description = "수량", example = "2")
    val quantity: Int,

    @field:Schema(description = "결제수단", example = "CARD")
    val paymentMethod: String,

    @field:Schema(description = "결제상태", example = "CONFIRMED")
    val paymentStatus: String,

    @field:Schema(description = "픽업시간", example = "14:00:00")
    val pickupTime: LocalTime
)
