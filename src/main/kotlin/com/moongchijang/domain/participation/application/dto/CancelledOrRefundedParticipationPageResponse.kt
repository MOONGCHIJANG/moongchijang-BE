package com.moongchijang.domain.participation.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "마이페이지 환불/취소 탭 참여 내역 페이지 응답")
data class CancelledOrRefundedParticipationPageResponse(

    @field:Schema(description = "목록 데이터")
    val content: List<CancelledOrRefundedParticipationItemResponse>,

    @field:Schema(description = "전체 데이터 수", example = "12")
    val totalElements: Long,

    @field:Schema(description = "전체 페이지 수", example = "2")
    val totalPages: Int,
) {
    companion object {
        fun from(page: Page<CancelledOrRefundedParticipationItemResponse>): CancelledOrRefundedParticipationPageResponse =
            CancelledOrRefundedParticipationPageResponse(
                content = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            )
    }
}
