package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyConfirmationStatus
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipantRole
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipationResponse
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipationStatus
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipationUpsertRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.time.kstNow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional
class TogetherBuyParticipationCommandService(
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    fun upsert(
        groupBuyId: Long,
        userId: Long,
        request: TogetherBuyParticipationUpsertRequest,
    ): TogetherBuyParticipationResponse {
        val groupBuy = findAvailableRoom(groupBuyId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val existing = participationRepository.findByUserIdAndGroupBuyId(userId, groupBuyId)

        if (groupBuy.isSoloRoom && existing == null) {
            throw CustomException(ErrorCode.GROUPBUY_NOT_RECRUITING)
        }

        val existingQuantity = existing?.quantity ?: 0
        val availableForUser = groupBuy.maxQuantity - (groupBuy.currentQuantity - existingQuantity)
        if (availableForUser < 1) {
            throw CustomException(ErrorCode.GROUPBUY_SOLD_OUT)
        }

        val quantity = minOf(request.quantity, availableForUser)
        val quantityAdjusted = quantity != request.quantity
        val participation = existing?.also {
            it.changeQuantity(quantity, groupBuy.price)
        } ?: Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = quantity,
            productAmount = groupBuy.price * quantity,
            feeAmount = 0,
            totalAmount = groupBuy.price * quantity,
        )

        groupBuy.adjustCurrentQuantityBy(quantity - existingQuantity)
        val saved = participationRepository.save(participation)
        return saved.toResponse(groupBuy, quantityAdjusted)
    }

    fun cancel(groupBuyId: Long, userId: Long) {
        val groupBuy = findAvailableRoom(groupBuyId)
        val participation = participationRepository.findByUserIdAndGroupBuyId(userId, groupBuyId)
            ?: throw CustomException(ErrorCode.PARTICIPATION_NOT_FOUND)

        if (groupBuy.organizer?.id == userId) {
            throw CustomException(ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED)
        }

        groupBuy.adjustCurrentQuantityBy(-participation.quantity)
        participationRepository.delete(participation)
    }

    private fun findAvailableRoom(groupBuyId: Long): GroupBuy {
        val groupBuy = groupBuyRepository.findWithLockById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }
        if (groupBuy.status != GroupBuyStatus.IN_PROGRESS) {
            throw CustomException(ErrorCode.GROUPBUY_NOT_RECRUITING)
        }
        if (!groupBuy.deadline.isAfter(clock.kstNow())) {
            throw CustomException(ErrorCode.GROUPBUY_DEADLINE_PASSED)
        }
        return groupBuy
    }

    private fun Participation.toResponse(
        groupBuy: GroupBuy,
        quantityAdjusted: Boolean,
    ) = TogetherBuyParticipationResponse(
        participationId = id,
        userId = requireNotNull(user.id),
        nickname = user.nickname,
        role = if (groupBuy.organizer?.id == user.id) {
            TogetherBuyParticipantRole.ORGANIZER
        } else {
            TogetherBuyParticipantRole.PARTICIPANT
        },
        status = TogetherBuyParticipationStatus.JOINED,
        quantity = quantity,
        productAmount = productAmount,
        confirmationStatus = TogetherBuyConfirmationStatus.NOT_REQUESTED,
        quantityAdjusted = quantityAdjusted,
    )
}
