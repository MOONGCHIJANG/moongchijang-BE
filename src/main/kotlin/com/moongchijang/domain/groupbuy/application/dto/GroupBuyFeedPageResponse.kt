package com.moongchijang.domain.groupbuy.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "공구 목록 페이지 응답")
data class GroupBuyFeedPageResponse(

    @field:Schema(description = "목록 데이터")
    val content: List<GroupBuyFeedItemResponse>,

    @field:Schema(description = "현재 페이지(1-base)", example = "1")
    val page: Int,

    @field:Schema(description = "페이지 크기", example = "20")
    val size: Int,

    @field:Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int,

    @field:Schema(description = "전체 데이터 수", example = "92")
    val totalElements: Long,

    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean
) {
    companion object {
        fun from(page: Page<GroupBuyFeedItemResponse>): GroupBuyFeedPageResponse {
            return GroupBuyFeedPageResponse(
                content = page.content,
                page = page.number + 1,
                size = page.size,
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                hasNext = page.hasNext()
            )
        }
    }
}
