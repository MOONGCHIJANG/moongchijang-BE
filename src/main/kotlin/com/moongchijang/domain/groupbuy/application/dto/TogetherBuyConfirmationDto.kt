package com.moongchijang.domain.groupbuy.application.dto

import java.time.LocalDateTime

data class TogetherBuyConfirmationRequest(
    val confirmationDeadline: LocalDateTime? = null,
)

data class TogetherBuyConfirmationResponse(
    val groupBuyId: Long,
    val status: TogetherBuyConfirmationRequestStatus,
    val confirmationDeadline: LocalDateTime,
    val confirmedParticipantCount: Int,
    val pendingParticipantCount: Int,
)

enum class TogetherBuyConfirmationRequestStatus {
    REQUESTED,
}
