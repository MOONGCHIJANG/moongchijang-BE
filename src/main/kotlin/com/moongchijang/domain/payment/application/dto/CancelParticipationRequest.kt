package com.moongchijang.domain.payment.application.dto

import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import jakarta.validation.constraints.Size

data class CancelParticipationRequest(
    val reason: ParticipationCancelReason,

    @field:Size(max = 500)
    val reasonDetail: String? = null,
)
