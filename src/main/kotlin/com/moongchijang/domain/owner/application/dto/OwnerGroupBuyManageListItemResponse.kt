package com.moongchijang.domain.owner.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "사장님 공구 관리 목록 아이템 응답")
data class OwnerGroupBuyManageListItemResponse(
    @field:Schema(description = "공구 ID (실제 공구 항목일 때만 값 존재)", example = "101", nullable = true)
    val groupBuyId: Long? = null,

    @field:Schema(description = "공구 개설 요청 ID (승인대기 항목일 때만 값 존재)", example = "55", nullable = true)
    val requestId: Long? = null,

    @field:Schema(description = "공구명", example = "두쫀쿠 세트")
    val productName: String,

    @field:Schema(description = "공구 금액", example = "9900")
    val price: Int,

    @field:Schema(description = "픽업일", example = "2026-06-01")
    val pickupDate: LocalDate,

    @field:Schema(description = "마감 D-day (모집중 탭 전용)", example = "3", nullable = true)
    val deadlineDday: Int? = null,

    @field:Schema(description = "달성률(%)", example = "60", nullable = true)
    val achievementRate: Int? = null,

    @field:Schema(description = "현재 참여 수량", example = "12", nullable = true)
    val currentQuantity: Int? = null,

    @field:Schema(description = "목표 수량", example = "20", nullable = true)
    val targetQuantity: Int? = null,

    @field:Schema(description = "공구 상태", example = "IN_PROGRESS")
    val status: OwnerGroupBuyManageFilterType
)
