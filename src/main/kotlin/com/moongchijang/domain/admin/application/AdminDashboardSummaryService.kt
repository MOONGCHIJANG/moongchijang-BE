package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.AdminDashboardSummaryResponse
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOrderStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.time.kstNow
import com.moongchijang.global.time.kstToday
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AdminDashboardSummaryService(
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(AdminDashboardSummaryService::class.java)

    fun getSummary(): AdminDashboardSummaryResponse {
        log.info("[AdminDashboardSummaryService] 관리자 대시보드 요약 조회 시작")
        val now = clock.kstNow()
        val today = clock.kstToday()
        val todayRange = today.toRange()
        val yesterdayRange = today.minusDays(1).toRange()
        val pendingApprovalStatuses = listOf(GroupBuyRequestStatus.IN_REVIEW, GroupBuyRequestStatus.IN_CONTACT)
        val completedApprovalStatuses = listOf(GroupBuyRequestStatus.OPENED, GroupBuyRequestStatus.REJECTED)
        val orderOverdueBefore = now.minusHours(48)
        val unconfirmedOrderOver48hCount = groupBuyRepository.countOverdueAdminOrders(
            status = GroupBuyStatus.ACHIEVED,
            orderStatus = GroupBuyOrderStatus.PENDING,
            overdueBefore = orderOverdueBefore
        )

        val pendingRefundAmount = participationRepository.sumPaymentOrderAmountByStatus(ParticipationStatus.REFUND_PENDING)
        val todayPendingRefundAmount = participationRepository.sumPaymentOrderAmountByStatusAndCancelledAtBetween(
            status = ParticipationStatus.REFUND_PENDING,
            from = todayRange.start,
            to = todayRange.end
        )
        val yesterdayPendingRefundAmount = participationRepository.sumPaymentOrderAmountByStatusAndCancelledAtBetween(
            status = ParticipationStatus.REFUND_PENDING,
            from = yesterdayRange.start,
            to = yesterdayRange.end
        )
        val pendingCreatedAts = groupBuyRequestRepository.findCreatedAtByStatusIn(pendingApprovalStatuses)

        val response = AdminDashboardSummaryResponse(
            pendingRefundAmount = pendingRefundAmount,
            pendingRefundAmountChangeRate = changeRate(todayPendingRefundAmount, yesterdayPendingRefundAmount),
            pendingApprovalCount = groupBuyRequestRepository.countByStatusIn(pendingApprovalStatuses),
            averageReviewMinutes = averageReviewMinutes(pendingCreatedAts, now),
            pendingApprovalChangeRate = changeRate(
                current = groupBuyRequestRepository.countByStatusInAndCreatedAtBetween(
                    statuses = pendingApprovalStatuses,
                    from = todayRange.start,
                    to = todayRange.end
                ),
                previous = groupBuyRequestRepository.countByStatusInAndCreatedAtBetween(
                    statuses = pendingApprovalStatuses,
                    from = yesterdayRange.start,
                    to = yesterdayRange.end
                )
            ),
            unconfirmedOrderCount = groupBuyRepository.countByStatusAndOrderStatus(
                status = GroupBuyStatus.ACHIEVED,
                orderStatus = GroupBuyOrderStatus.PENDING
            ),
            unconfirmedOrderOver48hCount = unconfirmedOrderOver48hCount,
            todayCompletedRefundCount = participationRepository.countByStatusAndRefundedAtFromUntil(
                status = ParticipationStatus.REFUNDED,
                from = todayRange.start,
                to = todayRange.end
            ),
            todayCompletedApprovalCount = groupBuyRequestStatusHistoryRepository.countByStatusInAndChangedAtFromUntil(
                statuses = completedApprovalStatuses,
                from = todayRange.start,
                to = todayRange.end
            ),
            hasOrderOver48h = unconfirmedOrderOver48hCount > 0
        )
        log.info(
            "[AdminDashboardSummaryService] 관리자 대시보드 요약 조회 완료: pendingApprovalCount={}, unconfirmedOrderCount={}",
            response.pendingApprovalCount,
            response.unconfirmedOrderCount,
        )
        return response
    }

    private fun averageReviewMinutes(createdAts: List<LocalDateTime>, now: LocalDateTime): Long {
        if (createdAts.isEmpty()) {
            return 0L
        }

        return createdAts
            .map { Duration.between(it, now).toMinutes().coerceAtLeast(0) }
            .average()
            .toLong()
    }

    private fun changeRate(current: Long, previous: Long): Double {
        if (previous == 0L) {
            return if (current == 0L) 0.0 else 100.0
        }

        return (current - previous) * 100.0 / previous
    }

    private fun LocalDate.toRange(): DateTimeRange =
        DateTimeRange(
            start = atStartOfDay(),
            end = plusDays(1).atStartOfDay()
        )

    private data class DateTimeRange(
        val start: LocalDateTime,
        val end: LocalDateTime,
    )
}
