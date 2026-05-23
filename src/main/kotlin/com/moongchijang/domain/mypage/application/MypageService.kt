package com.moongchijang.domain.mypage.application

import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.mypage.application.dto.MypageGroupBuyRequestResponse
import com.moongchijang.domain.mypage.application.dto.MypageParticipationResponse
import com.moongchijang.domain.mypage.application.dto.MypageParticipationStatusFilter
import com.moongchijang.domain.mypage.application.dto.MypageRefundResponse
import com.moongchijang.domain.mypage.application.dto.MypageSummaryResponse
import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import com.moongchijang.domain.payment.domain.repository.PaymentOrderRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MypageService(
    private val participationRepository: ParticipationRepository,
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val paymentOrderRepository: PaymentOrderRepository
) {

    fun getSummary(userId: Long): MypageSummaryResponse =
        MypageSummaryResponse(
            activeCount = participationRepository.countByUserIdAndStatusInAndPickupStatusIn(
                userId = userId,
                statuses = ACTIVE_PARTICIPATION_STATUSES,
                pickupStatuses = ACTIVE_PICKUP_STATUSES
            ),
            completedCount = participationRepository.countByUserIdAndStatusInAndPickupStatus(
                userId = userId,
                statuses = ACTIVE_PARTICIPATION_STATUSES,
                pickupStatus = PickupStatus.PICKED_UP
            ),
            refundedCount = participationRepository.countByUserIdAndStatusIn(
                userId = userId,
                statuses = REFUND_PARTICIPATION_STATUSES
            ),
            requestCount = groupBuyRequestRepository.countByUserId(userId)
        )

    fun getRefunds(userId: Long): List<MypageRefundResponse> =
        participationRepository
            .findByUserIdAndStatusInOrderByRefundedAtDescCreatedAtDesc(userId, REFUND_PARTICIPATION_STATUSES)
            .map(MypageRefundResponse::from)

    fun getParticipations(
        userId: Long,
        status: MypageParticipationStatusFilter
    ): List<MypageParticipationResponse> =
        when (status) {
            MypageParticipationStatusFilter.ACTIVE -> getActiveParticipations(userId)
            MypageParticipationStatusFilter.COMPLETED -> getCompletedParticipations(userId)
            MypageParticipationStatusFilter.REFUNDED -> getRefundedParticipations(userId)
        }

    fun getActiveParticipations(userId: Long): List<MypageParticipationResponse> {
        val participations = participationRepository.findByUserIdAndStatusInAndPickupStatusInOrderByCreatedAtDesc(
            userId = userId,
            statuses = ACTIVE_PARTICIPATION_STATUSES,
            pickupStatuses = ACTIVE_PICKUP_STATUSES
        )
        if (participations.isEmpty()) return emptyList()

        val cancellableGroupBuyIds = approvedPaymentGroupBuyIds(userId)
        return participations.map { MypageParticipationResponse.from(it, cancellableGroupBuyIds) }
    }

    fun getCompletedParticipations(userId: Long): List<MypageParticipationResponse> =
        participationRepository
            .findByUserIdAndStatusInAndPickupStatusOrderByPickedUpAtDescCreatedAtDesc(
                userId = userId,
                statuses = ACTIVE_PARTICIPATION_STATUSES,
                pickupStatus = PickupStatus.PICKED_UP
            )
            .map(MypageParticipationResponse::from)

    fun getRefundedParticipations(userId: Long): List<MypageParticipationResponse> =
        participationRepository
            .findByUserIdAndStatusInOrderByCreatedAtDesc(userId, REFUND_PARTICIPATION_STATUSES)
            .map(MypageParticipationResponse::from)

    fun getGroupBuyRequests(userId: Long): List<MypageGroupBuyRequestResponse> =
        groupBuyRequestRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .map(MypageGroupBuyRequestResponse::from)

    private fun approvedPaymentGroupBuyIds(userId: Long): Set<Long> =
        paymentOrderRepository.findGroupBuyIdsByUserIdAndStatus(userId, PaymentOrderStatus.APPROVED).toSet()

    private companion object {
        val ACTIVE_PARTICIPATION_STATUSES = listOf(
            ParticipationStatus.PAID_WAITING_GOAL,
            ParticipationStatus.CONFIRMED
        )
        val ACTIVE_PICKUP_STATUSES = listOf(
            PickupStatus.NOT_READY,
            PickupStatus.READY
        )
        val REFUND_PARTICIPATION_STATUSES = listOf(
            ParticipationStatus.REFUND_PENDING,
            ParticipationStatus.REFUNDED
        )
    }
}
