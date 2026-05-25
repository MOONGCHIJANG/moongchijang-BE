package com.moongchijang.domain.mypage.application

import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.mypage.application.dto.MypageGroupBuyRequestResponse
import com.moongchijang.domain.mypage.application.dto.MypageParticipationPaymentInfo
import com.moongchijang.domain.mypage.application.dto.MypageParticipationResponse
import com.moongchijang.domain.mypage.application.dto.MypageParticipationStatusFilter
import com.moongchijang.domain.mypage.application.dto.MypageRefundResponse
import com.moongchijang.domain.mypage.application.dto.MypageSummaryResponse
import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import com.moongchijang.domain.payment.domain.repository.PaymentOrderRepository
import com.moongchijang.domain.participation.domain.entity.Participation
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

        val paymentInfoByParticipationId = paymentInfoByParticipationId(participations)
        val cancellableGroupBuyIds = paymentInfoByParticipationId.values
            .filter { it.isApproved }
            .map { it.groupBuyId }
            .toSet()
        return participations.map {
            MypageParticipationResponse.from(
                participation = it,
                approvedPaymentGroupBuyIds = cancellableGroupBuyIds,
                paymentInfo = paymentInfoByParticipationId[it.id]
            )
        }
    }

    fun getPickupWaitingParticipations(userId: Long): List<MypageParticipationResponse> =
        participationRepository
            .findByUserIdAndStatusInAndPickupStatusInOrderByCreatedAtDesc(
                userId = userId,
                statuses = PICKUP_WAITING_PARTICIPATION_STATUSES,
                pickupStatuses = PICKUP_WAITING_PICKUP_STATUSES
            )
            .let(::withPaymentInfo)

    fun getPickupCompletedParticipations(userId: Long): List<MypageParticipationResponse> =
        participationRepository
            .findByUserIdAndStatusInAndPickupStatusOrderByPickedUpAtDescCreatedAtDesc(
                userId = userId,
                statuses = PICKUP_COMPLETED_PARTICIPATION_STATUSES,
                pickupStatus = PickupStatus.PICKED_UP
            )
            .let(::withPaymentInfo)

    fun getCancelledOrRefundedParticipations(userId: Long): List<MypageParticipationResponse> =
        participationRepository
            .findByUserIdAndStatusInOrderByCreatedAtDesc(userId, CANCELLED_OR_REFUNDED_PARTICIPATION_STATUSES)
            .let(::withPaymentInfo)

    fun getGroupBuyRequests(userId: Long): List<MypageGroupBuyRequestResponse> =
        groupBuyRequestRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .map(MypageGroupBuyRequestResponse::from)

    private fun withPaymentInfo(participations: List<Participation>): List<MypageParticipationResponse> {
        val paymentInfoByParticipationId = paymentInfoByParticipationId(participations)
        return participations.map {
            MypageParticipationResponse.from(
                participation = it,
                paymentInfo = paymentInfoByParticipationId[it.id]
            )
        }
    }

    private fun paymentInfoByParticipationId(participations: List<Participation>): Map<Long, MypageParticipationPaymentInfo> {
        if (participations.isEmpty()) return emptyMap()
        return paymentOrderRepository.findPaymentSummariesByParticipationIdIn(
            participationIds = participations.map { it.id }
        ).associate {
            it.participationId to MypageParticipationPaymentInfo(
                groupBuyId = it.groupBuyId,
                isApproved = it.orderStatus == PaymentOrderStatus.APPROVED,
                paidAt = it.paidAt,
                paymentMethod = it.paymentMethod
            )
        }
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
