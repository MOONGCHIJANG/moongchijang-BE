package com.moongchijang.domain.participation.application.dto

import jakarta.validation.constraints.Min

data class ParticipationCreateRequest(
    @field:Min(1, message = "수량은 1개 이상이어야 합니다.")
    val quantity: Int
)
