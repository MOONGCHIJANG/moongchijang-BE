package com.moongchijang.domain.participation.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "마이페이지 진행 중 탭 참여 내역 페이지 응답")
data class InProgressParticipationPageResponse(

    @field:Schema(description = "목록 데이터")
    val content: List<InProgressParticipationItemResponse>,

    @field:Schema(description = "전체 데이터 수", example = "24")
    val totalElements: Long,

    @field:Schema(description = "전체 페이지 수", example = "3")
    val totalPages: Int
) {
    companion object {
        fun from(page: Page<InProgressParticipationItemResponse>): InProgressParticipationPageResponse {
            return InProgressParticipationPageResponse(
                content = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
        }
    }
}
