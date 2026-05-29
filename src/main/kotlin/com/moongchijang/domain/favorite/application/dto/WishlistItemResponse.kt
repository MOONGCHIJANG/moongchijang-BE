package com.moongchijang.domain.favorite.application.dto

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressCalculator
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Schema(description = "찜 목록 카드 아이템")
data class WishlistItemResponse(

    @field:Schema(description = "공구 ID", example = "101")
    val groupBuyId: Long,

    @field:Schema(description = "대표 썸네일 URL")
    val thumbnailUrl: String?,

    @field:Schema(description = "마감 D-day 숫자", example = "3")
    val dDay: Int,

    @field:Schema(description = "마감 D-day 라벨", example = "D-3")
    val dDayLabel: String,

    @field:Schema(description = "매장명", example = "사이드템포")
    val storeName: String,

    @field:Schema(description = "지역 라벨", example = "서울")
    val regionLabel: String,

    @field:Schema(description = "상품명", example = "두쭌쿠 오리지널 1개")
    val productName: String,

    @field:Schema(description = "픽업 날짜 원본", example = "2026-05-25")
    val pickupDate: LocalDate,

    @field:Schema(description = "픽업 날짜 표시 문자열", example = "5/25(월)")
    val pickupDateLabel: String,

    @field:Schema(description = "마감 일시 원본", example = "2026-05-24T21:00:00")
    val deadline: LocalDateTime,

    @field:Schema(description = "마감 일시 표시 문자열", example = "5/24(일) 21:00")
    val deadlineLabel: String,

    @field:Schema(description = "달성률(%)", example = "72")
    val achievementRate: Int,

    @field:Schema(description = "가격", example = "18000")
    val price: Int,

    @field:Schema(description = "현재 참여 인원 수", example = "36")
    val currentParticipantCount: Int,

    @field:Schema(description = "목표 참여 인원 수", example = "50")
    val targetParticipantCount: Int,

    @field:Schema(description = "찜 여부", example = "true")
    val isWishlisted: Boolean,
) {
    companion object {
        fun from(
            groupBuy: GroupBuy,
            thumbnailUrl: String?,
            now: LocalDateTime = LocalDateTime.now()
        ): WishlistItemResponse {
            val dDay = ChronoUnit.DAYS.between(now.toLocalDate(), groupBuy.deadline.toLocalDate()).toInt()
            val achievementRate = GroupBuyProgressCalculator.achievementRate(
                groupBuy.currentQuantity,
                groupBuy.targetQuantity
            )

            return WishlistItemResponse(
                groupBuyId = groupBuy.id,
                thumbnailUrl = thumbnailUrl,
                dDay = dDay,
                dDayLabel = if (groupBuy.deadline <= now) "마감" else if (dDay == 0) "D-day" else "D-$dDay",
                storeName = groupBuy.store.name,
                regionLabel = groupBuy.store.region.label,
                productName = groupBuy.productName,
                pickupDate = groupBuy.pickupDate,
                pickupDateLabel = formatPickupDateLabel(groupBuy.pickupDate),
                deadline = groupBuy.deadline,
                deadlineLabel = formatDeadlineLabel(groupBuy.deadline),
                achievementRate = achievementRate,
                price = groupBuy.price,
                currentParticipantCount = groupBuy.currentQuantity,
                targetParticipantCount = groupBuy.targetQuantity,
                isWishlisted = true,
            )
        }

        private fun formatPickupDateLabel(date: LocalDate): String {
            val day = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
            return "${date.monthValue}/${date.dayOfMonth}($day)"
        }

        private fun formatDeadlineLabel(deadline: LocalDateTime): String {
            return deadline.format(DateTimeFormatter.ofPattern("M/d(E) HH:mm", Locale.KOREAN))
        }
    }
}
