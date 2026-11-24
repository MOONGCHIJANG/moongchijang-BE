package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyConfirmationRequest
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyConfirmationRequestStatus
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyConfirmationResponse
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationConfirmation
import com.moongchijang.domain.participation.domain.repository.ParticipationConfirmationRepository
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.time.kstNow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
class TogetherBuyConfirmationCommandService(
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val participationConfirmationRepository: ParticipationConfirmationRepository,
    private val clock: Clock,
) {
    fun request(
        groupBuyId: Long,
        userId: Long,
        request: TogetherBuyConfirmationRequest?,
    ): TogetherBuyConfirmationResponse {
        val groupBuy = groupBuyRepository.findWithLockById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }
        val now = clock.kstNow()

        validateRequestable(groupBuy, userId, now)
        val confirmationDeadline = resolveConfirmationDeadline(groupBuy, request?.confirmationDeadline, now)
        val participations = participationRepository.findAllActiveWithUserByGroupBuyId(groupBuy.id)

        groupBuy.requestParticipantConfirmation(confirmationDeadline)
        participations.forEach { it.requestConfirmation() }
        participationConfirmationRepository.saveAll(
            participations.map {
                ParticipationConfirmation(
                    participation = it,
                    requestedAt = now,
                    deadline = confirmationDeadline,
                )
            },
        )

        return TogetherBuyConfirmationResponse(
            groupBuyId = groupBuy.id,
            status = TogetherBuyConfirmationRequestStatus.REQUESTED,
            confirmationDeadline = confirmationDeadline,
            confirmedParticipantCount = 0,
            pendingParticipantCount = participations.size,
        )
    }

    private fun validateRequestable(groupBuy: GroupBuy, userId: Long, now: LocalDateTime) {
        if (groupBuy.organizer?.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
        if (groupBuy.isParticipantConfirmationRequested()) {
            throw CustomException(ErrorCode.GROUPBUY_CONFIRMATION_ALREADY_REQUESTED)
        }
        if (groupBuy.status != GroupBuyStatus.IN_PROGRESS || !groupBuy.deadline.isAfter(now)) {
            throw CustomException(ErrorCode.GROUPBUY_CONFIRMATION_NOT_AVAILABLE)
        }
        val minimumQuantity = groupBuy.minQuantity ?: groupBuy.targetQuantity
        if (groupBuy.currentQuantity < minimumQuantity) {
            throw CustomException(ErrorCode.GROUPBUY_CONFIRMATION_NOT_AVAILABLE)
        }
    }

    private fun resolveConfirmationDeadline(
        groupBuy: GroupBuy,
        requestedDeadline: LocalDateTime?,
        now: LocalDateTime,
    ): LocalDateTime {
        val deadline = requestedDeadline ?: minOf(now.plusHours(DEFAULT_CONFIRMATION_HOURS), groupBuy.deadline)
        if (!deadline.isAfter(now) || deadline.isAfter(groupBuy.deadline)) {
            throw CustomException(ErrorCode.GROUPBUY_CONFIRMATION_DEADLINE_INVALID)
        }
        return deadline
    }

    private companion object {
        const val DEFAULT_CONFIRMATION_HOURS = 1L
    }
}
