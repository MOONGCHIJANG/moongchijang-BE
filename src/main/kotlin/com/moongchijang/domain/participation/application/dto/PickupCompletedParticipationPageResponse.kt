package com.moongchijang.domain.participation.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "마이페이지 픽업 완료 탭 참여 내역 페이지 응답")
data class PickupCompletedParticipationPageResponse(

    @field:Schema(description = "목록 데이터")
    val content: List<PickupCompletedParticipationItemResponse>,

    @field:Schema(description = "전체 데이터 수", example = "12")
    val totalElements: Long,

    @field:Schema(description = "전체 페이지 수", example = "2")
    val totalPages: Int,
) {
    companion object {
        fun from(page: Page<PickupCompletedParticipationItemResponse>): PickupCompletedParticipationPageResponse =
            PickupCompletedParticipationPageResponse(
                content = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            )
    }
}
