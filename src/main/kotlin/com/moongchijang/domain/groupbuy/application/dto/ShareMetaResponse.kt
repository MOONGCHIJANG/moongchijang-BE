package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "공구 공유 메타데이터 응답")
data class ShareMetaResponse(

    @field:Schema(description = "공유용 상세 URL", example = "https://moongchijang.com/group-buys/101")
    val shareUrl: String,

    @field:Schema(description = "공유 카드 제목", example = "두쭌쿠 오리지널 1개")
    val title: String,

    @field:Schema(description = "공유 카드 설명", example = "지금 함께 주문하고 픽업해요.")
    val description: String,

    @field:Schema(description = "공유 카드 대표 이미지 URL", example = "https://cdn.moongchijang.com/group-buys/101/thumbnail.jpg")
    val imageUrl: String?,

    @field:Schema(description = "매장명", example = "사이드템포")
    val storeName: String,

    @field:Schema(description = "마감 일시", example = "2026-05-10T23:59:00")
    val deadline: LocalDateTime,

    @field:Schema(description = "픽업 날짜", example = "2026-05-15")
    val pickupDate: LocalDate,

    @field:Schema(description = "픽업 시작 시간", nullable = true, example = "14:00:00")
    val pickupTimeStart: LocalTime?,

    @field:Schema(description = "픽업 종료 시간", nullable = true, example = "18:00:00")
    val pickupTimeEnd: LocalTime?
) {
    companion object {
        fun from(
            groupBuy: GroupBuy,
            shareUrl: String,
            description: String
        ): ShareMetaResponse {
            return ShareMetaResponse(
                shareUrl = shareUrl,
                title = groupBuy.productName,
                description = description,
                imageUrl = groupBuy.thumbnailUrl,
                storeName = groupBuy.store.name,
                deadline = groupBuy.deadline,
                pickupDate = groupBuy.pickupDate,
                pickupTimeStart = groupBuy.pickupTimeStart,
                pickupTimeEnd = groupBuy.pickupTimeEnd
            )
        }
    }
}
