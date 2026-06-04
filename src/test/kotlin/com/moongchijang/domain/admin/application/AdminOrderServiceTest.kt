package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.AdminOrderStatusFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOrderStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.GroupBuyPendingRefundCount
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
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
import java.util.Optional

class AdminOrderServiceTest {

    private val groupBuyRepository: GroupBuyRepository = mock(GroupBuyRepository::class.java)
    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val storeStaffRepository: StoreStaffRepository = mock(StoreStaffRepository::class.java)
    private val notificationEventPublisher: NotificationEventPublisher = mock(NotificationEventPublisher::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-27T04:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminOrderService(
        groupBuyRepository = groupBuyRepository,
        participationRepository = participationRepository,
        storeStaffRepository = storeStaffRepository,
        notificationEventPublisher = notificationEventPublisher,
        clock = clock
    )

    @Test
    fun `48시간 초과 발주 목록을 조회한다`() {
        val pageable = PageRequest.of(0, 20)
        val now = LocalDateTime.of(2026, 5, 27, 13, 0)
        val achievedAt = now.minusHours(50)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 30L,
            status = GroupBuyStatus.ACHIEVED,
            targetQuantity = 20,
            currentQuantity = 20
        ).apply {
            this.achievedAt = achievedAt
            orderStatus = GroupBuyOrderStatus.PENDING
        }
        val pendingRefundCount = pendingRefundCount(groupBuyId = 30L, count = 2L)

        `when`(
            groupBuyRepository.findAdminOrderPage(
                GroupBuyStatus.ACHIEVED,
                listOf(GroupBuyOrderStatus.PENDING),
                now.minusHours(48),
                pageable
            )
        ).thenReturn(PageImpl(listOf(groupBuy), pageable, 1))
        `when`(participationRepository.countPendingRefundsByGroupBuyIdIn(listOf(30L)))
            .thenReturn(listOf(pendingRefundCount))

        val result = service.getOrders(AdminOrderStatusFilter.OVERDUE_48H, pageable)

        assertEquals(1, result.content.size)
        assertEquals(30L, result.content[0].orderId)
        assertEquals("두쫀쿠 1개", result.content[0].productName)
        assertEquals(achievedAt, result.content[0].achievedAt)
        assertEquals(20, result.content[0].finalQuantity)
        assertEquals(2L, result.content[0].pendingRefundCount)
        assertEquals(50L, result.content[0].elapsedHours)
        assertEquals(100, result.content[0].progressRate)
        assertEquals(GroupBuyOrderStatus.PENDING, result.content[0].orderStatus)
        assertTrue(result.content[0].actionable)
    }

    @Test
    fun `목록이 비어있으면 환불 대기 집계를 생략한다`() {
        val pageable = PageRequest.of(0, 20)
        `when`(
            groupBuyRepository.findAdminOrderPage(
                GroupBuyStatus.ACHIEVED,
                GroupBuyOrderStatus.entries,
                null,
                pageable
            )
        ).thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = service.getOrders(AdminOrderStatusFilter.ALL, pageable)

        assertTrue(result.content.isEmpty())
        verifyNoInteractions(participationRepository)
    }

    @Test
    fun `발주 상세를 조회한다`() {
        val now = LocalDateTime.of(2026, 5, 27, 13, 0)
        val achievedAt = now.minusHours(3)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 31L,
            status = GroupBuyStatus.ACHIEVED,
            targetQuantity = 10,
            currentQuantity = 8
        ).apply {
            this.achievedAt = achievedAt
        }
        `when`(groupBuyRepository.findAdminOrderDetailById(31L)).thenReturn(Optional.of(groupBuy))
        `when`(
            participationRepository.countByGroupBuyIdAndStatusIn(
                31L,
                listOf(ParticipationStatus.REFUND_PENDING)
            )
        ).thenReturn(1L)

        val result = service.getOrderDetail(31L)

        assertEquals(31L, result.orderId)
        assertEquals("뭉치장 베이커리", result.storeName)
        assertEquals(1L, result.pendingRefundCount)
        assertEquals(3L, result.elapsedHours)
        assertEquals(80, result.progressRate)
        assertTrue(result.actionable)
    }

    @Test
    fun `완료된 공구도 발주 상세를 조회할 수 있고 작업은 비활성화된다`() {
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 35L,
            status = GroupBuyStatus.COMPLETED,
            targetQuantity = 10,
            currentQuantity = 10
        ).apply {
            orderStatus = GroupBuyOrderStatus.PENDING
        }
        `when`(groupBuyRepository.findAdminOrderDetailById(35L)).thenReturn(Optional.of(groupBuy))
        `when`(
            participationRepository.countByGroupBuyIdAndStatusIn(
                35L,
                listOf(ParticipationStatus.REFUND_PENDING)
            )
        ).thenReturn(0L)

        val result = service.getOrderDetail(35L)

        assertEquals(35L, result.orderId)
        assertFalse(result.actionable)
    }

    @Test
    fun `사장님 연락 완료를 기록한다`() {
        val now = LocalDateTime.of(2026, 5, 27, 13, 0)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 32L,
            status = GroupBuyStatus.ACHIEVED
        ).apply {
            achievedAt = now.minusHours(1)
            orderStatus = GroupBuyOrderStatus.PENDING
        }
        `when`(groupBuyRepository.findWithLockById(32L)).thenReturn(Optional.of(groupBuy))
        `when`(
            participationRepository.countByGroupBuyIdAndStatusIn(
                32L,
                listOf(ParticipationStatus.REFUND_PENDING)
            )
        ).thenReturn(0L)

        val result = service.markOwnerContacted(32L)

        assertEquals(now, result.ownerContactedAt)
        assertEquals(GroupBuyOrderStatus.PENDING, result.orderStatus)
        assertTrue(result.actionable)
    }

    @Test
    fun `발주를 확정 처리한다`() {
        val now = LocalDateTime.of(2026, 5, 27, 13, 0)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 33L,
            status = GroupBuyStatus.ACHIEVED
        ).apply {
            achievedAt = now.minusHours(1)
            orderStatus = GroupBuyOrderStatus.PENDING
        }
        `when`(groupBuyRepository.findWithLockById(33L)).thenReturn(Optional.of(groupBuy))
        `when`(
            participationRepository.countByGroupBuyIdAndStatusIn(
                33L,
                listOf(ParticipationStatus.REFUND_PENDING)
            )
        ).thenReturn(0L)

        val result = service.confirmOrder(33L)

        assertEquals(GroupBuyOrderStatus.CONFIRMED, result.orderStatus)
        assertEquals(now, result.orderConfirmedAt)
        assertFalse(result.actionable)
    }

    @Test
    fun `확정된 발주는 다시 취소 처리할 수 없다`() {
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 34L,
            status = GroupBuyStatus.ACHIEVED
        ).apply {
            orderStatus = GroupBuyOrderStatus.CONFIRMED
        }
        `when`(groupBuyRepository.findWithLockById(34L)).thenReturn(Optional.of(groupBuy))

        assertThrows(CustomException::class.java) {
            service.cancelOrder(34L)
        }
    }

    @Test
    fun `발주를 취소하면 사장님 알림을 발행한다`() {
        val now = LocalDateTime.of(2026, 5, 27, 13, 0)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 36L,
            status = GroupBuyStatus.ACHIEVED
        ).apply {
            achievedAt = now.minusHours(1)
            orderStatus = GroupBuyOrderStatus.PENDING
        }
        `when`(groupBuyRepository.findWithLockById(36L)).thenReturn(Optional.of(groupBuy))
        `when`(
            participationRepository.countByGroupBuyIdAndStatusIn(
                36L,
                listOf(ParticipationStatus.REFUND_PENDING)
            )
        ).thenReturn(0L)
        `when`(storeStaffRepository.findUserIdsByStoreId(groupBuy.store.id)).thenReturn(listOf(201L, 202L))

        val result = service.cancelOrder(36L)

        assertEquals(GroupBuyOrderStatus.CANCELLED, result.orderStatus)
        assertEquals(now, result.orderCancelledAt)
        org.mockito.Mockito.verify(notificationEventPublisher)
            .publishOwnerOrderCancelled(36L, listOf(201L, 202L), now)
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
