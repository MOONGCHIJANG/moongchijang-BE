package com.moongchijang.domain.groupbuy.application.dto

import jakarta.validation.constraints.Min

data class TogetherBuyParticipationUpsertRequest(
    @field:Min(1)
    val quantity: Int,
)

data class TogetherBuyParticipationResponse(
    val participationId: Long,
    val userId: Long,
    val nickname: String?,
    val role: TogetherBuyParticipantRole,
    val status: TogetherBuyParticipationStatus,
    val quantity: Int,
    val productAmount: Int,
    val confirmationStatus: TogetherBuyConfirmationStatus,
    val quantityAdjusted: Boolean,
)

data class TogetherBuyParticipantRosterResponse(
    val organizerParticipationId: Long,
    val participants: List<TogetherBuyParticipationResponse>,
)

enum class TogetherBuyParticipantRole {
    ORGANIZER,
    PARTICIPANT,
}

enum class TogetherBuyParticipationStatus {
    JOINED,
}

enum class TogetherBuyConfirmationStatus {
    NOT_REQUESTED,
}
