package com.moongchijang.domain.mypage.application

import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.participation.application.dto.CancelledOrRefundedParticipationItemResponse
import com.moongchijang.domain.participation.application.dto.CancelledOrRefundedParticipationPageResponse
import com.moongchijang.domain.participation.application.dto.InProgressParticipationItemResponse
import com.moongchijang.domain.participation.application.dto.InProgressParticipationPageResponse
import com.moongchijang.domain.participation.application.dto.MyPageTabCountsResponse
import com.moongchijang.domain.participation.application.dto.PickupCompletedParticipationItemResponse
import com.moongchijang.domain.participation.application.dto.PickupCompletedParticipationPageResponse
import com.moongchijang.domain.participation.application.dto.PickupWaitingParticipationItemResponse
import com.moongchijang.domain.participation.application.dto.PickupWaitingParticipationPageResponse
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MyPageParticipationQueryService(
    private val participationRepository: ParticipationRepository,
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getTabCounts(userId: Long): MyPageTabCountsResponse {
        val response = MyPageTabCountsResponse(
            inProgressCount = participationRepository.countByUserIdAndStatusIn(userId, IN_PROGRESS_STATUSES),
            pickupWaitingCount = participationRepository.countByUserIdAndStatusInAndPickupStatusIn(
                userId = userId,
                participationStatuses = PICKUP_WAITING_PARTICIPATION_STATUSES,
                pickupStatuses = PICKUP_WAITING_PICKUP_STATUSES,
            ),
            pickupCompletedCount = participationRepository.countByUserIdAndStatusInAndPickupStatusIn(
                userId = userId,
                participationStatuses = PICKUP_COMPLETED_PARTICIPATION_STATUSES,
                pickupStatuses = PICKUP_COMPLETED_PICKUP_STATUSES,
            ),
            cancelledOrRefundedCount = participationRepository.countByUserIdAndStatusIn(
                userId = userId,
                statuses = CANCELLED_OR_REFUNDED_STATUSES,
            ),
            requestCount = groupBuyRequestRepository.countByUserId(userId),
        )

        log.info(
            "[MyPageParticipationQueryService] 마이페이지 탭 카운트 조회 완료: userId={}, inProgress={}, pickupWaiting={}, pickupCompleted={}, cancelledOrRefunded={}, request={}",
            userId,
            response.inProgressCount,
            response.pickupWaitingCount,
            response.pickupCompletedCount,
            response.cancelledOrRefundedCount,
            response.requestCount,
        )
        return response
    }

    @Transactional(readOnly = true)
    fun getInProgressParticipations(userId: Long, pageable: Pageable): InProgressParticipationPageResponse {
        log.info(
            "[MyPageParticipationQueryService] 진행 중 참여 내역 조회 시작: userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )

        val page = participationRepository.findInProgressByUserId(
            userId = userId,
            statuses = IN_PROGRESS_STATUSES,
            pageable = pageable
        )

        log.info(
            "[MyPageParticipationQueryService] 진행 중 참여 내역 조회 완료: userId={}, contentSize={}, totalElements={}, totalPages={}",
            userId,
            page.content.size,
            page.totalElements,
            page.totalPages
        )

        val now = LocalDateTime.now()
        val mapped = page.map { participation -> InProgressParticipationItemResponse.from(participation, now) }
        return InProgressParticipationPageResponse.from(mapped)
    }

    @Transactional(readOnly = true)
    fun getPickupWaitingParticipations(userId: Long, pageable: Pageable): PickupWaitingParticipationPageResponse {
        log.info(
            "[MyPageParticipationQueryService] 픽업 대기 참여 내역 조회 시작: userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )

        val page = participationRepository.findPickupWaitingByUserId(
            userId = userId,
            participationStatuses = PICKUP_WAITING_PARTICIPATION_STATUSES,
            pickupStatuses = PICKUP_WAITING_PICKUP_STATUSES,
            pageable = pageable
        )

        log.info(
            "[MyPageParticipationQueryService] 픽업 대기 참여 내역 조회 완료: userId={}, contentSize={}, totalElements={}, totalPages={}",
            userId,
            page.content.size,
            page.totalElements,
            page.totalPages
        )

        val mapped = page.map { participation -> PickupWaitingParticipationItemResponse.from(participation) }
        return PickupWaitingParticipationPageResponse.from(mapped)
    }

    @Transactional(readOnly = true)
    fun getPickupCompletedParticipations(userId: Long, pageable: Pageable): PickupCompletedParticipationPageResponse {
        log.info(
            "[MyPageParticipationQueryService] 픽업 완료 참여 내역 조회 시작: userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )

        val page = participationRepository.findPickupCompletedByUserId(
            userId = userId,
            participationStatuses = PICKUP_COMPLETED_PARTICIPATION_STATUSES,
            pickupStatuses = PICKUP_COMPLETED_PICKUP_STATUSES,
            pageable = pageable
        )

        val mapped = page.map { participation -> PickupCompletedParticipationItemResponse.from(participation) }
        return PickupCompletedParticipationPageResponse.from(mapped)
    }

    @Transactional(readOnly = true)
    fun getCancelledOrRefundedParticipations(
        userId: Long,
        pageable: Pageable
    ): CancelledOrRefundedParticipationPageResponse {
        log.info(
            "[MyPageParticipationQueryService] 환불/취소 참여 내역 조회 시작: userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )

        val page = participationRepository.findCancelledOrRefundedByUserId(
            userId = userId,
            statuses = CANCELLED_OR_REFUNDED_STATUSES,
            pageable = pageable
        )

        val mapped = page.map { participation -> CancelledOrRefundedParticipationItemResponse.from(participation) }
        return CancelledOrRefundedParticipationPageResponse.from(mapped)
    }

    companion object {
        private val IN_PROGRESS_STATUSES = listOf(
            ParticipationStatus.PAID_WAITING_GOAL
        )
        private val PICKUP_WAITING_PARTICIPATION_STATUSES = listOf(
            ParticipationStatus.CONFIRMED
        )
        private val PICKUP_WAITING_PICKUP_STATUSES = listOf(
            PickupStatus.NOT_READY,
            PickupStatus.READY
        )
        private val PICKUP_COMPLETED_PARTICIPATION_STATUSES = listOf(
            ParticipationStatus.CONFIRMED
        )
        private val PICKUP_COMPLETED_PICKUP_STATUSES = listOf(
            PickupStatus.PICKED_UP
        )
        private val CANCELLED_OR_REFUNDED_STATUSES = listOf(
            ParticipationStatus.CANCELLED,
            ParticipationStatus.REFUNDED
        )
    }
}
