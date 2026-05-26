package com.moongchijang.domain.owner.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Schema(description = "사장님 공구 기간 연장 요청")
data class OwnerGroupBuyExtensionRequest(
    @field:NotNull(message = "연장 희망 마감일시는 필수입니다")
    @field:Schema(description = "연장 희망 마감일시", example = "2026-06-05T23:59:00")
    val extendedDeadline: LocalDateTime
)
