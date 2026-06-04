package com.moongchijang.domain.favorite.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.global.time.TimePolicy
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.time.LocalDateTime

@Schema(description = "찜 목록 페이지 응답")
data class WishlistPageResponse(

    @field:Schema(description = "찜 카드 목록")
    val content: List<WishlistItemResponse>,

    @field:Schema(description = "전체 데이터 수", example = "24")
    val totalElements: Long,

    @field:Schema(description = "전체 페이지 수", example = "3")
    val totalPages: Int,

    @field:Schema(description = "현재 페이지 번호(0-base)", example = "0")
    val number: Int,

    @field:Schema(description = "페이지 크기", example = "20")
    val size: Int,

    @field:Schema(description = "마감 24시간 이내 찜 건수", example = "2")
    val urgentCount: Int,
) {
    companion object {
        fun from(
            page: Page<GroupBuy>,
            thumbnailUrlResolver: (GroupBuy) -> String?,
            now: LocalDateTime = LocalDateTime.now(TimePolicy.BUSINESS_ZONE_ID),
            urgentCount: Int,
        ): WishlistPageResponse {
            return WishlistPageResponse(
                content = page.content.map { WishlistItemResponse.from(it, thumbnailUrlResolver(it), now) },
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size,
                urgentCount = urgentCount,
            )
        }
    }
}
