package com.moongchijang.domain.admin.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AdminDashboardSummaryServiceTest {

    private val groupBuyRequestRepository: GroupBuyRequestRepository = mock(GroupBuyRequestRepository::class.java)
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository =
        mock(GroupBuyRequestStatusHistoryRepository::class.java)
    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-27T04:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminDashboardSummaryService(
        groupBuyRequestRepository = groupBuyRequestRepository,
        groupBuyRequestStatusHistoryRepository = groupBuyRequestStatusHistoryRepository,
        participationRepository = participationRepository,
        clock = clock
    )

    @Test
    fun `운영 관리 요약을 현재 도메인 기준으로 집계한다`() {
        val todayStart = LocalDateTime.of(2026, 5, 27, 0, 0)
        val tomorrowStart = LocalDateTime.of(2026, 5, 28, 0, 0)
        val yesterdayStart = LocalDateTime.of(2026, 5, 26, 0, 0)
        val now = LocalDateTime.of(2026, 5, 27, 13, 0)
        val pendingStatuses = listOf(GroupBuyRequestStatus.IN_REVIEW, GroupBuyRequestStatus.IN_CONTACT)
        val completedStatuses = listOf(GroupBuyRequestStatus.OPENED, GroupBuyRequestStatus.REJECTED)

        `when`(participationRepository.sumPaymentOrderAmountByStatus(ParticipationStatus.REFUND_PENDING))
            .thenReturn(30_000L)
        `when`(
            participationRepository.sumPaymentOrderAmountByStatusAndCancelledAtBetween(
                ParticipationStatus.REFUND_PENDING,
                todayStart,
                tomorrowStart
            )
        ).thenReturn(15_000L)
        `when`(
            participationRepository.sumPaymentOrderAmountByStatusAndCancelledAtBetween(
                ParticipationStatus.REFUND_PENDING,
                yesterdayStart,
                todayStart
            )
        ).thenReturn(10_000L)
        `when`(groupBuyRequestRepository.findCreatedAtByStatusIn(pendingStatuses))
            .thenReturn(listOf(now.minusMinutes(30), now.minusMinutes(90)))
        `when`(groupBuyRequestRepository.countByStatusIn(pendingStatuses)).thenReturn(4L)
        `when`(groupBuyRequestRepository.countByStatusInAndCreatedAtBetween(pendingStatuses, todayStart, tomorrowStart))
            .thenReturn(3L)
        `when`(groupBuyRequestRepository.countByStatusInAndCreatedAtBetween(pendingStatuses, yesterdayStart, todayStart))
            .thenReturn(2L)
        `when`(
            participationRepository.countByStatusAndRefundedAtBetween(
                ParticipationStatus.REFUNDED,
                todayStart,
                tomorrowStart
            )
        ).thenReturn(5L)
        `when`(
            groupBuyRequestStatusHistoryRepository.countByStatusInAndChangedAtBetween(
                completedStatuses,
                todayStart,
                tomorrowStart
            )
        ).thenReturn(6L)

        val result = service.getSummary()

        assertEquals(30_000L, result.pendingRefundAmount)
        assertEquals(50.0, result.pendingRefundAmountChangeRate)
        assertEquals(4L, result.pendingApprovalCount)
        assertEquals(60L, result.averageReviewMinutes)
        assertEquals(50.0, result.pendingApprovalChangeRate)
        assertEquals(0L, result.unconfirmedOrderCount)
        assertEquals(0L, result.unconfirmedOrderOver48hCount)
        assertEquals(5L, result.todayCompletedRefundCount)
        assertEquals(6L, result.todayCompletedApprovalCount)
        assertFalse(result.hasOrderOver48h)
    }

    @Test
    fun `전일 값이 없으면 현재 값 존재 여부에 따라 증감률을 반환한다`() {
        val todayStart = LocalDateTime.of(2026, 5, 27, 0, 0)
        val tomorrowStart = LocalDateTime.of(2026, 5, 28, 0, 0)
        val yesterdayStart = LocalDateTime.of(2026, 5, 26, 0, 0)
        val pendingStatuses = listOf(GroupBuyRequestStatus.IN_REVIEW, GroupBuyRequestStatus.IN_CONTACT)

        `when`(participationRepository.sumPaymentOrderAmountByStatus(ParticipationStatus.REFUND_PENDING))
            .thenReturn(0L)
        `when`(
            participationRepository.sumPaymentOrderAmountByStatusAndCancelledAtBetween(
                ParticipationStatus.REFUND_PENDING,
                todayStart,
                tomorrowStart
            )
        ).thenReturn(1_000L)
        `when`(
            participationRepository.sumPaymentOrderAmountByStatusAndCancelledAtBetween(
                ParticipationStatus.REFUND_PENDING,
                yesterdayStart,
                todayStart
            )
        ).thenReturn(0L)
        `when`(groupBuyRequestRepository.findCreatedAtByStatusIn(pendingStatuses)).thenReturn(emptyList())
        `when`(groupBuyRequestRepository.countByStatusIn(pendingStatuses)).thenReturn(0L)
        `when`(groupBuyRequestRepository.countByStatusInAndCreatedAtBetween(pendingStatuses, todayStart, tomorrowStart))
            .thenReturn(0L)
        `when`(groupBuyRequestRepository.countByStatusInAndCreatedAtBetween(pendingStatuses, yesterdayStart, todayStart))
            .thenReturn(0L)

        val result = service.getSummary()

        assertEquals(100.0, result.pendingRefundAmountChangeRate)
        assertEquals(0.0, result.pendingApprovalChangeRate)
        assertEquals(0L, result.averageReviewMinutes)
        verify(participationRepository).countByStatusAndRefundedAtBetween(
            ParticipationStatus.REFUNDED,
            todayStart,
            tomorrowStart
        )
    }
}
