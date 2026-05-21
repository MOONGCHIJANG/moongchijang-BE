package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Schema(description = "공구 피드 카드 응답")
data class GroupBuyFeedItemResponse(

    @field:Schema(description = "공구 ID", example = "101")
    val id: Long,

    @field:Schema(description = "대표 이미지 URL")
    val thumbnailUrl: String?,

    @field:Schema(description = "마감 D-day 숫자", example = "3")
    val dDay: Int,

    @field:Schema(description = "마감 D-day 라벨", example = "D-3")
    val dDayLabel: String,

    @field:Schema(description = "매장명", example = "사이드템포")
    val storeName: String,

    @field:Schema(description = "시/도 코드", example = "SEOUL")
    val regionType: RegionType,

    @field:Schema(description = "시/도 한글 라벨", example = "서울")
    val regionLabel: String,

    @field:Schema(description = "세부지역 코드", example = "SEOUL_GANGNAM_YEOKSAM_SAMSEONG")
    val districtType: DistrictType,

    @field:Schema(description = "세부지역 한글 라벨", example = "강남 | 역삼 | 삼성")
    val districtLabel: String,

    @field:Schema(description = "상품명", example = "두쭌쿠 오리지널 1개")
    val productName: String,

    @field:Schema(description = "픽업 날짜 라벨", example = "4/15(화)")
    val pickupDateLabel: String,

    @field:Schema(description = "마감 일시", example = "2026-05-10T23:59:00")
    val deadline: LocalDateTime,

    @field:Schema(description = "가격", example = "18000")
    val price: Int,

    @field:Schema(description = "달성률(%)", example = "72")
    val achievementRate: Int,

    @field:Schema(description = "현재 수량", example = "36")
    val currentQuantity: Int,

    @field:Schema(description = "목표 수량", example = "50")
    val targetQuantity: Int
) {
    companion object {
        fun from(groupBuy: GroupBuy, now: LocalDateTime = LocalDateTime.now()): GroupBuyFeedItemResponse {
            val dDay = ChronoUnit.DAYS.between(now.toLocalDate(), groupBuy.deadline.toLocalDate()).toInt()
            val rate = GroupBuyProgressCalculator.achievementRate(groupBuy.currentQuantity, groupBuy.targetQuantity)

            return GroupBuyFeedItemResponse(
                id = groupBuy.id,
                thumbnailUrl = groupBuy.thumbnailUrl,
                dDay = dDay,
                dDayLabel = "D-$dDay",
                storeName = groupBuy.store.name,
                regionType = groupBuy.store.region,
                regionLabel = groupBuy.store.region.label,
                districtType = groupBuy.store.district,
                districtLabel = groupBuy.store.district.label,
                productName = groupBuy.productName,
                pickupDateLabel = formatPickupDateLabel(groupBuy.pickupDate),
                deadline = groupBuy.deadline,
                price = groupBuy.price,
                achievementRate = rate,
                currentQuantity = groupBuy.currentQuantity,
                targetQuantity = groupBuy.targetQuantity
            )
        }

        private fun formatPickupDateLabel(date: LocalDate): String {
            val day = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
            return "${date.monthValue}/${date.dayOfMonth}($day)"
        }
    }
}
