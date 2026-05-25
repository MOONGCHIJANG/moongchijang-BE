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
            inProgressCount = participationRepository.countByUserIdAndStatusInAndPickupStatusIn(
                userId = userId,
                statuses = IN_PROGRESS_PARTICIPATION_STATUSES,
                pickupStatuses = IN_PROGRESS_PICKUP_STATUSES
            ),
            pickupWaitingCount = participationRepository.countByUserIdAndStatusInAndPickupStatusIn(
                userId = userId,
                statuses = PICKUP_WAITING_PARTICIPATION_STATUSES,
                pickupStatuses = PICKUP_WAITING_PICKUP_STATUSES
            ),
            pickupCompletedCount = participationRepository.countByUserIdAndStatusInAndPickupStatus(
                userId = userId,
                statuses = PICKUP_COMPLETED_PARTICIPATION_STATUSES,
                pickupStatus = PickupStatus.PICKED_UP
            ),
            cancelledOrRefundedCount = participationRepository.countByUserIdAndStatusIn(
                userId = userId,
                statuses = CANCELLED_OR_REFUNDED_PARTICIPATION_STATUSES
            ),
            requestCount = groupBuyRequestRepository.countByUserId(userId)
        )

    fun getRefunds(userId: Long): List<MypageRefundResponse> =
        participationRepository
            .findByUserIdAndStatusInOrderByRefundedAtDescCreatedAtDesc(
                userId = userId,
                statuses = REFUND_PARTICIPATION_STATUSES,
                refundPendingStatus = ParticipationStatus.REFUND_PENDING
            )
            .map(MypageRefundResponse::from)

    fun getParticipations(
        userId: Long,
        status: MypageParticipationStatusFilter
    ): List<MypageParticipationResponse> =
        when (status) {
            MypageParticipationStatusFilter.IN_PROGRESS -> getInProgressParticipations(userId)
            MypageParticipationStatusFilter.PICKUP_WAITING -> getPickupWaitingParticipations(userId)
            MypageParticipationStatusFilter.PICKUP_COMPLETED -> getPickupCompletedParticipations(userId)
            MypageParticipationStatusFilter.CANCELLED_OR_REFUNDED -> getCancelledOrRefundedParticipations(userId)
        }

    fun getInProgressParticipations(userId: Long): List<MypageParticipationResponse> {
        val participations = participationRepository.findByUserIdAndStatusInAndPickupStatusInOrderByCreatedAtDesc(
            userId = userId,
            statuses = IN_PROGRESS_PARTICIPATION_STATUSES,
            pickupStatuses = IN_PROGRESS_PICKUP_STATUSES
        )
        if (participations.isEmpty()) return emptyList()

        val inProgressGroupBuyIds = participations.map { it.groupBuy.id }.toSet()
        val cancellableGroupBuyIds = approvedPaymentGroupBuyIds(userId, inProgressGroupBuyIds)
        return participations.map { MypageParticipationResponse.from(it, cancellableGroupBuyIds) }
    }

    fun getPickupWaitingParticipations(userId: Long): List<MypageParticipationResponse> =
        participationRepository
            .findByUserIdAndStatusInAndPickupStatusInOrderByCreatedAtDesc(
                userId = userId,
                statuses = PICKUP_WAITING_PARTICIPATION_STATUSES,
                pickupStatuses = PICKUP_WAITING_PICKUP_STATUSES
            )
            .map(MypageParticipationResponse::from)

    fun getPickupCompletedParticipations(userId: Long): List<MypageParticipationResponse> =
        participationRepository
            .findByUserIdAndStatusInAndPickupStatusOrderByPickedUpAtDescCreatedAtDesc(
                userId = userId,
                statuses = PICKUP_COMPLETED_PARTICIPATION_STATUSES,
                pickupStatus = PickupStatus.PICKED_UP
            )
            .map(MypageParticipationResponse::from)

    fun getCancelledOrRefundedParticipations(userId: Long): List<MypageParticipationResponse> =
        participationRepository
            .findByUserIdAndStatusInOrderByCreatedAtDesc(userId, CANCELLED_OR_REFUNDED_PARTICIPATION_STATUSES)
            .map(MypageParticipationResponse::from)

    fun getGroupBuyRequests(userId: Long): List<MypageGroupBuyRequestResponse> =
        groupBuyRequestRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .map(MypageGroupBuyRequestResponse::from)

    private fun approvedPaymentGroupBuyIds(userId: Long, groupBuyIds: Collection<Long>): Set<Long> {
        if (groupBuyIds.isEmpty()) return emptySet()
        return paymentOrderRepository.findGroupBuyIdsByUserIdAndStatusAndGroupBuyIdIn(
            userId = userId,
            status = PaymentOrderStatus.APPROVED,
            groupBuyIds = groupBuyIds
        ).toSet()
    }

    private companion object {
        val IN_PROGRESS_PARTICIPATION_STATUSES = listOf(
            ParticipationStatus.PAID_WAITING_GOAL
        )
        val ACTIVE_PICKUP_STATUSES = listOf(
            PickupStatus.NOT_READY,
            PickupStatus.READY
        )
        val IN_PROGRESS_PICKUP_STATUSES = ACTIVE_PICKUP_STATUSES
        val PICKUP_WAITING_PARTICIPATION_STATUSES = listOf(
            ParticipationStatus.CONFIRMED
        )
        val PICKUP_WAITING_PICKUP_STATUSES = ACTIVE_PICKUP_STATUSES
        val PICKUP_COMPLETED_PARTICIPATION_STATUSES = PICKUP_WAITING_PARTICIPATION_STATUSES
        val REFUND_PARTICIPATION_STATUSES = listOf(
            ParticipationStatus.REFUND_PENDING,
            ParticipationStatus.REFUNDED
        )
        val CANCELLED_OR_REFUNDED_PARTICIPATION_STATUSES =
            listOf(ParticipationStatus.CANCELLED) + REFUND_PARTICIPATION_STATUSES
    }
}
