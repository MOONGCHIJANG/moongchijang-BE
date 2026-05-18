package com.moongchijang.domain.payment.application.dto

import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import java.time.LocalDateTime

data class ConfirmPaymentResponse(
    val paymentId: String,
    val participationId: Long,
    val participationStatus: ParticipationStatus,
    val displayStatus: String,
    val amount: Int,
    val method: String?,
    val approvedAt: LocalDateTime,
)
