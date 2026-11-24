package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.participation.domain.entity.ParticipationConfirmationStatus
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
    PENDING,
    CONFIRMED,
    DECLINED,
    EXCLUDED,
}

fun ParticipationConfirmationStatus.toTogetherBuyConfirmationStatus(): TogetherBuyConfirmationStatus = when (this) {
    ParticipationConfirmationStatus.NOT_REQUESTED -> TogetherBuyConfirmationStatus.NOT_REQUESTED
    ParticipationConfirmationStatus.REQUESTED -> TogetherBuyConfirmationStatus.PENDING
    ParticipationConfirmationStatus.CONFIRMED -> TogetherBuyConfirmationStatus.CONFIRMED
    ParticipationConfirmationStatus.AUTO_EXCLUDED -> TogetherBuyConfirmationStatus.EXCLUDED
}
