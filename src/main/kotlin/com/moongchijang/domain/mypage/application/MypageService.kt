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
import com.moongchijang.global.time.kstToday
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class MypageService(
    private val participationRepository: ParticipationRepository,
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(MypageService::class.java)

    fun getSummary(userId: Long): MypageSummaryResponse {
        log.info("[MypageService] 마이페이지 요약 조회 시작: userId={}", userId)
        val response = MypageSummaryResponse(
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
            requestCount = groupBuyRequestRepository.countByUser_Id(userId)
        )
        log.info("[MypageService] 마이페이지 요약 조회 완료: userId={}", userId)
        return response
    }

    fun getRefunds(userId: Long): List<MypageRefundResponse> {
        log.info("[MypageService] 환불 목록 조회 시작: userId={}", userId)
        val participations = participationRepository.findByUserIdAndStatusInOrderByRefundedAtDescCreatedAtDesc(
            userId = userId,
            statuses = REFUND_PARTICIPATION_STATUSES,
            refundPendingStatus = ParticipationStatus.REFUND_PENDING
        )
        val paymentInfoByParticipationId = paymentInfoByParticipationId(participations)

        val responses = participations.map {
            MypageRefundResponse.from(
                participation = it,
                thumbnailUrl = s3ImageReferenceResolver.resolveForRead(it.groupBuy.thumbnailKey),
                paymentInfo = paymentInfoByParticipationId[it.id]
            )
        }
        log.info("[MypageService] 환불 목록 조회 완료: userId={}, count={}", userId, responses.size)
        return responses
    }

    fun getParticipations(
        userId: Long,
        status: MypageParticipationStatusFilter
    ): List<MypageParticipationResponse> {
        log.info("[MypageService] 참여 목록 조회 시작: userId={}, status={}", userId, status)
        val responses = when (status) {
            MypageParticipationStatusFilter.IN_PROGRESS -> getInProgressParticipations(userId)
            MypageParticipationStatusFilter.PICKUP_WAITING -> getPickupWaitingParticipations(userId)
            MypageParticipationStatusFilter.PICKUP_COMPLETED -> getPickupCompletedParticipations(userId)
            MypageParticipationStatusFilter.CANCELLED_OR_REFUNDED -> getCancelledOrRefundedParticipations(userId)
        }
        log.info("[MypageService] 참여 목록 조회 완료: userId={}, status={}, count={}", userId, status, responses.size)
        return responses
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
        val today = clock.kstToday()
        return participations.map {
            MypageParticipationResponse.from(
                participation = it,
                thumbnailUrl = s3ImageReferenceResolver.resolveForRead(it.groupBuy.thumbnailKey),
                approvedPaymentGroupBuyIds = cancellableGroupBuyIds,
                paymentInfo = paymentInfoByParticipationId[it.id],
                today = today,
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

    fun getGroupBuyRequests(userId: Long): List<MypageGroupBuyRequestResponse> {
        log.info("[MypageService] 요청공구 목록 조회 시작: userId={}", userId)
        val responses = groupBuyRequestRepository
            .findByUser_IdOrderByCreatedAtDesc(userId)
            .map(MypageGroupBuyRequestResponse::from)
        log.info("[MypageService] 요청공구 목록 조회 완료: userId={}, count={}", userId, responses.size)
        return responses
    }

    private fun withPaymentInfo(participations: List<Participation>): List<MypageParticipationResponse> {
        val paymentInfoByParticipationId = paymentInfoByParticipationId(participations)
        val today = clock.kstToday()
        return participations.map {
            MypageParticipationResponse.from(
                participation = it,
                thumbnailUrl = s3ImageReferenceResolver.resolveForRead(it.groupBuy.thumbnailKey),
                paymentInfo = paymentInfoByParticipationId[it.id],
                today = today,
            )
        }
    }

    private fun paymentInfoByParticipationId(participations: List<Participation>): Map<Long, MypageParticipationPaymentInfo> {
        if (participations.isEmpty()) return emptyMap()
        return paymentOrderRepository.findPaymentSummariesByParticipationIdIn(
            participationIds = participations.map { it.id }
        ).groupBy { it.participationId }
            .mapValues { (_, summaries) ->
                val summary = summaries.find { it.orderStatus == PaymentOrderStatus.APPROVED }
                    ?: summaries.first()
                MypageParticipationPaymentInfo(
                    groupBuyId = summary.groupBuyId,
                    isApproved = summary.orderStatus == PaymentOrderStatus.APPROVED,
                    paidAt = summary.paidAt,
                    paymentMethod = summary.paymentMethod
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
