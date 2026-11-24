package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipantRole
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipantRosterResponse
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipationResponse
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipationStatus
import com.moongchijang.domain.groupbuy.application.dto.toTogetherBuyConfirmationStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TogetherBuyParticipantRosterQueryService(
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
) {
    fun getRoster(groupBuyId: Long, userId: Long): TogetherBuyParticipantRosterResponse {
        val groupBuy = groupBuyRepository.findWithStoreById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }
        val participants = participationRepository.findAllActiveWithUserByGroupBuyId(groupBuyId)
        val organizerId = groupBuy.organizer?.id
            ?: throw CustomException(ErrorCode.GROUPBUY_NOT_FOUND)

        if (organizerId != userId && participants.none { it.user.id == userId }) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val organizerParticipation = participants.firstOrNull { it.user.id == organizerId }
            ?: throw CustomException(ErrorCode.PARTICIPATION_NOT_FOUND)
        val orderedParticipants = listOf(organizerParticipation) +
            participants.filterNot { it.user.id == organizerId }

        return TogetherBuyParticipantRosterResponse(
            organizerParticipationId = organizerParticipation.id,
            participants = orderedParticipants.map { it.toResponse(organizerId) },
        )
    }

    private fun Participation.toResponse(organizerId: Long) =
        TogetherBuyParticipationResponse(
            participationId = id,
            userId = requireNotNull(user.id),
            nickname = user.nickname,
            role = if (user.id == organizerId) {
                TogetherBuyParticipantRole.ORGANIZER
            } else {
                TogetherBuyParticipantRole.PARTICIPANT
            },
            status = TogetherBuyParticipationStatus.JOINED,
            quantity = quantity,
            productAmount = productAmount,
            confirmationStatus = confirmationStatus.toTogetherBuyConfirmationStatus(),
            quantityAdjusted = false,
        )
}
