package com.moongchijang.domain.payment.application.dto

import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import java.time.LocalDateTime

data class CancelParticipationResponse(
    val participationId: Long,
    val status: ParticipationStatus,
    val cancelledAt: LocalDateTime,
    val refundedAt: LocalDateTime,
)
