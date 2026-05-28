package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyImage
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Schema(description = "공구 상세 응답")
data class GroupBuyDetailResponse(

    @field:Schema(example = "101")
    val id: Long,

    @field:Schema(example = "뭉치장 베이커리")
    val storeName: String,

    @field:Schema(example = "SEOUL")
    val regionType: RegionType,

    @field:Schema(example = "서울")
    val regionLabel: String,

    @field:Schema(example = "SEOUL_GANGNAM_YEOKSAM_SAMSEONG")
    val districtType: DistrictType,

    @field:Schema(example = "강남 | 역삼 | 삼성")
    val districtLabel: String,

    @field:Schema(example = "버터떡 1개")
    val productName: String,

    @field:Schema(example = "버터떡은 맛있어요.")
    val productDescription: String,

    @field:Schema(description = "대표 이미지 URL")
    val thumbnailUrl: String?,

    @field:Schema(description = "상세 이미지 URL 목록")
    val imageUrls: List<String>,

    @field:Schema(example = "6000")
    val price: Int,

    @field:Schema(example = "72")
    val achievementRate: Int,

    @field:Schema(example = "36")
    val currentQuantity: Int,

    @field:Schema(example = "50")
    val targetQuantity: Int,

    @field:Schema(nullable = true, example = "100")
    val maxQuantity: Int?,

    @field:Schema(example = "2026-05-10T23:59:00")
    val deadline: LocalDateTime,

    @field:Schema(example = "2026-05-15")
    val pickupDate: LocalDate,

    @field:Schema(example = "14:00:00")
    val pickupTimeStart: LocalTime,

    @field:Schema(example = "18:00:00")
    val pickupTimeEnd: LocalTime,

    @field:Schema(example = "4/15(화)")
    val pickupDateLabel: String,

    @field:Schema(example = "4/15(화) 14:00~18:00")
    val pickupDateTimeLabel: String,

    @field:Schema(example = "5/10 23:59")
    val deadlineDateTimeLabel: String,

    @field:Schema(example = "서울 강남구 OO길 1")
    val pickupLocation: String,

    val pickupLatitude: Double?,
    val pickupLongitude: Double?,

    @field:Schema(example = "3")
    val dDay: Int,

    @field:Schema(example = "D-3")
    val dDayLabel: String,

    @field:Schema(example = "true")
    val isWishlisted: Boolean,

    @field:Schema(example = "false")
    val isClosed: Boolean,

    @field:Schema(example = "false")
    val isParticipated: Boolean,

    @field:Schema(example = "true")
    val canParticipate: Boolean
) {
    companion object {
        fun from(
            groupBuy: GroupBuy,
            images: List<GroupBuyImage>,
            isWishlisted: Boolean,
            isParticipated: Boolean,
            canParticipate: Boolean,
            now: LocalDateTime = LocalDateTime.now()
        ): GroupBuyDetailResponse {
            val dDay = ChronoUnit.DAYS.between(now.toLocalDate(), groupBuy.deadline.toLocalDate()).toInt()
            val pickupDateLabel = formatPickupLabel(groupBuy.pickupDate)
            val rate = GroupBuyProgressCalculator.achievementRate(groupBuy.currentQuantity, groupBuy.targetQuantity)

            return GroupBuyDetailResponse(
                id = groupBuy.id,
                storeName = groupBuy.store.name,
                regionType = groupBuy.store.region,
                regionLabel = groupBuy.store.region.label,
                districtType = groupBuy.store.district,
                districtLabel = groupBuy.store.district.label,
                productName = groupBuy.productName,
                productDescription = groupBuy.productDescription,
                thumbnailUrl = groupBuy.thumbnailUrl,
                imageUrls = images.map { it.imageUrl },
                price = groupBuy.price,
                achievementRate = rate,
                currentQuantity = groupBuy.currentQuantity,
                targetQuantity = groupBuy.targetQuantity,
                maxQuantity = groupBuy.maxQuantity,
                deadline = groupBuy.deadline,
                pickupDate = groupBuy.pickupDate,
                pickupTimeStart = groupBuy.pickupTimeStart,
                pickupTimeEnd = groupBuy.pickupTimeEnd,
                pickupDateLabel = pickupDateLabel,
                pickupDateTimeLabel =
                    "$pickupDateLabel ${formatTimeLabel(groupBuy.pickupTimeStart)}~${formatTimeLabel(groupBuy.pickupTimeEnd)}",
                deadlineDateTimeLabel =
                    "${groupBuy.deadline.monthValue}/${groupBuy.deadline.dayOfMonth} ${formatTimeLabel(groupBuy.deadline.toLocalTime())}",
                pickupLocation = groupBuy.pickupLocation,
                pickupLatitude = groupBuy.store.latitude,
                pickupLongitude = groupBuy.store.longitude,
                dDay = dDay,
                dDayLabel = if (dDay == 0) "D-day" else "D-$dDay",
                isWishlisted = isWishlisted,
                isClosed = GroupBuyProgressCalculator.isClosed(groupBuy),
                isParticipated = isParticipated,
                canParticipate = canParticipate
            )
        }

        private fun formatPickupLabel(date: LocalDate): String {
            val day = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
            return "${date.monthValue}/${date.dayOfMonth}($day)"
        }

        private fun formatTimeLabel(time: LocalTime): String =
            time.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}
