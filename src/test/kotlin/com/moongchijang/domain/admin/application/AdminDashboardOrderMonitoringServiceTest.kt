package com.moongchijang.domain.admin.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOrderStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.repository.GroupBuyPendingRefundCount
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AdminDashboardOrderMonitoringServiceTest {

    private val groupBuyRepository: GroupBuyRepository = mock(GroupBuyRepository::class.java)
    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-27T04:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminDashboardOrderMonitoringService(
        groupBuyRepository = groupBuyRepository,
        participationRepository = participationRepository,
        clock = clock
    )

    @Test
    fun `대시보드 발주 미확정 모니터링 목록과 요약을 조회한다`() {
        val pageable = PageRequest.of(0, 5)
        val now = LocalDateTime.of(2026, 5, 27, 13, 0)
        val overdueBefore = now.minusHours(48)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 41L,
            status = GroupBuyStatus.ACHIEVED,
            targetQuantity = 20,
            currentQuantity = 20
        ).apply {
            achievedAt = now.minusHours(50)
            orderStatus = GroupBuyOrderStatus.PENDING
            markOrderOwnerContacted(now.minusHours(2))
        }
        `when`(
            groupBuyRepository.findAdminOrderPage(
                GroupBuyStatus.ACHIEVED,
                listOf(GroupBuyOrderStatus.PENDING),
                null,
                pageable
            )
        ).thenReturn(PageImpl(listOf(groupBuy), pageable, 1))
        `when`(participationRepository.countPendingRefundsByGroupBuyIdIn(listOf(41L)))
            .thenReturn(listOf(pendingRefundCount(41L, 2L)))
        `when`(
            groupBuyRepository.countOverdueAdminOrders(
                GroupBuyStatus.ACHIEVED,
                GroupBuyOrderStatus.PENDING,
                overdueBefore
            )
        ).thenReturn(1L)
        `when`(groupBuyRepository.countByStatusAndOrderStatus(GroupBuyStatus.ACHIEVED, GroupBuyOrderStatus.PENDING))
            .thenReturn(3L)

        val result = service.getUnconfirmedOrders(pageable)

        assertEquals(3L, result.totalUnconfirmedCount)
        assertEquals(1L, result.overdueCount)
        assertTrue(result.hasOverdue)
        assertEquals(1, result.content.size)
        assertEquals(41L, result.content[0].orderId)
        assertEquals(2L, result.content[0].pendingRefundCount)
        assertEquals(50L, result.content[0].elapsedHours)
        assertTrue(result.content[0].overdue)
        assertTrue(result.content[0].ownerContacted)
        assertEquals(100, result.content[0].progressRate)
    }

    @Test
    fun `목록이 비어있으면 환불 대기 집계를 생략한다`() {
        val pageable = PageRequest.of(0, 5)
        val now = LocalDateTime.of(2026, 5, 27, 13, 0)
        val overdueBefore = now.minusHours(48)
        `when`(
            groupBuyRepository.findAdminOrderPage(
                GroupBuyStatus.ACHIEVED,
                listOf(GroupBuyOrderStatus.PENDING),
                null,
                pageable
            )
        ).thenReturn(PageImpl(emptyList(), pageable, 0))
        `when`(
            groupBuyRepository.countOverdueAdminOrders(
                GroupBuyStatus.ACHIEVED,
                GroupBuyOrderStatus.PENDING,
                overdueBefore
            )
        ).thenReturn(0L)
        `when`(groupBuyRepository.countByStatusAndOrderStatus(GroupBuyStatus.ACHIEVED, GroupBuyOrderStatus.PENDING))
            .thenReturn(0L)

        val result = service.getUnconfirmedOrders(pageable)

        assertTrue(result.content.isEmpty())
        assertFalse(result.hasOverdue)
        verifyNoInteractions(participationRepository)
    }

    private fun pendingRefundCount(
        groupBuyId: Long,
        count: Long
    ): GroupBuyPendingRefundCount =
        object : GroupBuyPendingRefundCount {
            override val groupBuyId: Long = groupBuyId
            override val pendingRefundCount: Long = count
        }
}
