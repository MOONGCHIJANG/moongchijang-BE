package com.moongchijang.domain.mypage.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.support.ParticipationFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class MyPageParticipationQueryServiceTest {

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    private val service by lazy {
        MyPageParticipationQueryService(participationRepository, groupBuyRequestRepository)
    }

    @Test
    fun `탭별 카운트 조회 시 Figma 탭 기준으로 분리된 건수 반환`() {
        val userId = 1L

        `when`(
            participationRepository.countByUserIdAndStatusIn(
                userId,
                listOf(ParticipationStatus.PAID_WAITING_GOAL)
            )
        ).thenReturn(4L)
        `when`(
            participationRepository.countByUserIdAndStatusInAndPickupStatusIn(
                userId = userId,
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
                pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY)
            )
        ).thenReturn(2L)
        `when`(
            participationRepository.countByUserIdAndStatusInAndPickupStatusIn(
                userId = userId,
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
                pickupStatuses = listOf(PickupStatus.PICKED_UP)
            )
        ).thenReturn(1L)
        `when`(
            participationRepository.countByUserIdAndStatusIn(
                userId,
                listOf(ParticipationStatus.CANCELLED, ParticipationStatus.REFUNDED)
            )
        ).thenReturn(3L)
        `when`(groupBuyRequestRepository.countByUserId(userId)).thenReturn(5L)

        val result = service.getTabCounts(userId)

        assertEquals(4L, result.inProgressCount)
        assertEquals(2L, result.pickupWaitingCount)
        assertEquals(1L, result.pickupCompletedCount)
        assertEquals(3L, result.cancelledOrRefundedCount)
        assertEquals(5L, result.requestCount)
    }

    @Test
    fun `진행 중 참여 내역 조회 시 진행 중 상태 조건 페이지 반환`() {
        val userId = 1L
        val pageable = PageRequest.of(0, 20)
        val now = LocalDateTime.now()
        val participation = ParticipationFixture.createParticipation(
            participationId = 101L,
            groupBuyId = 501L,
            quantity = 2,
            totalAmount = 36000,
            currentQuantity = 36,
            targetQuantity = 50,
            deadline = now.plusDays(2),
            pickupDate = now.toLocalDate().plusDays(3),
            pickupTimeStart = LocalTime.of(14, 0),
            createdAt = now.minusDays(1)
        )
        val page = PageImpl(listOf(participation), pageable, 1)

        `when`(
            participationRepository.findInProgressByUserId(
                userId = userId,
                statuses = listOf(ParticipationStatus.PAID_WAITING_GOAL),
                pageable = pageable
            )
        ).thenReturn(page)

        val result = service.getInProgressParticipations(userId, pageable)

        verify(participationRepository).findInProgressByUserId(
            userId = userId,
            statuses = listOf(ParticipationStatus.PAID_WAITING_GOAL),
            pageable = pageable
        )

        assertEquals(1L, result.totalElements)
        assertEquals(1, result.totalPages)
        assertEquals(1, result.content.size)
        assertTrue(result.content.isNotEmpty())
    }

    @Test
    fun `진행 중 참여 내역 조회 결과 카드 DTO 매핑 반환`() {
        val userId = 2L
        val pageable = PageRequest.of(0, 10)
        val createdAt = LocalDateTime.of(2026, 4, 12, 10, 30)
        val pickupDate = LocalDate.of(2026, 4, 15)
        val pickupTimeStart = LocalTime.of(14, 0)
        val deadline = LocalDateTime.now().plusDays(2)

        val participation = ParticipationFixture.createParticipation(
            participationId = 202L,
            groupBuyId = 777L,
            quantity = 2,
            totalAmount = 36000,
            currentQuantity = 36,
            targetQuantity = 50,
            deadline = deadline,
            pickupDate = pickupDate,
            pickupTimeStart = pickupTimeStart,
            createdAt = createdAt
        )
        val page = PageImpl(listOf(participation), pageable, 1)

        `when`(
            participationRepository.findInProgressByUserId(
                userId = userId,
                statuses = listOf(ParticipationStatus.PAID_WAITING_GOAL),
                pageable = pageable
            )
        ).thenReturn(page)

        val result = service.getInProgressParticipations(userId, pageable)
        val item = result.content.first()

        assertEquals(202L, item.participationId)
        assertEquals(777L, item.groupBuyId)
        assertEquals("두쫀쿠 오리지널 1개", item.productName)
        assertEquals("사이드템포", item.storeName)
        assertEquals(LocalDateTime.of(pickupDate, pickupTimeStart), item.pickupAt)
        assertEquals(36000, item.paidAmount)
        assertEquals(2, item.quantity)
        assertEquals(72, item.achievementRate)
        assertEquals(createdAt, item.participatedAt)
    }

    @Test
    fun `픽업 대기 참여 내역 조회 시 상태 조건 페이지 반환`() {
        val userId = 3L
        val pageable = PageRequest.of(0, 20)
        val now = LocalDateTime.now()
        val participation = ParticipationFixture.createParticipation(
            participationId = 301L,
            groupBuyId = 901L,
            quantity = 1,
            totalAmount = 18000,
            currentQuantity = 50,
            targetQuantity = 50,
            deadline = now.plusDays(1),
            pickupDate = now.toLocalDate().plusDays(2),
            pickupTimeStart = LocalTime.of(13, 0),
            createdAt = now.minusDays(1),
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatus = PickupStatus.READY
        )
        val page = PageImpl(listOf(participation), pageable, 1)

        `when`(
            participationRepository.findPickupWaitingByUserId(
                userId = userId,
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
                pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                pageable = pageable
            )
        ).thenReturn(page)

        val result = service.getPickupWaitingParticipations(userId, pageable)

        verify(participationRepository).findPickupWaitingByUserId(
            userId = userId,
            participationStatuses = listOf(ParticipationStatus.CONFIRMED),
            pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
            pageable = pageable
        )

        assertEquals(1L, result.totalElements)
        assertEquals(1, result.totalPages)
        assertEquals(1, result.content.size)
        assertTrue(result.content.isNotEmpty())
    }

    @Test
    fun `픽업 대기 참여 내역 조회 결과 카드 DTO 매핑 반환`() {
        val userId = 4L
        val pageable = PageRequest.of(0, 10)
        val createdAt = LocalDateTime.of(2026, 4, 12, 10, 30)
        val pickupDate = LocalDate.of(2026, 4, 15)
        val pickupTimeStart = LocalTime.of(14, 0)
        val deadline = LocalDateTime.now().plusDays(2)

        val participation = ParticipationFixture.createParticipation(
            participationId = 402L,
            groupBuyId = 977L,
            quantity = 1,
            totalAmount = 18000,
            currentQuantity = 50,
            targetQuantity = 50,
            deadline = deadline,
            pickupDate = pickupDate,
            pickupTimeStart = pickupTimeStart,
            createdAt = createdAt,
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatus = PickupStatus.NOT_READY,
            groupBuyStatus = GroupBuyStatus.CLOSED
        )
        val page = PageImpl(listOf(participation), pageable, 1)

        `when`(
            participationRepository.findPickupWaitingByUserId(
                userId = userId,
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
                pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                pageable = pageable
            )
        ).thenReturn(page)

        val result = service.getPickupWaitingParticipations(userId, pageable)
        val item = result.content.first()

        assertEquals(402L, item.participationId)
        assertEquals(977L, item.groupBuyId)
        assertEquals("두쫀쿠 오리지널 1개", item.productName)
        assertEquals("사이드템포", item.storeName)
        assertEquals(LocalDateTime.of(pickupDate, pickupTimeStart), item.pickupAt)
        assertEquals(18000, item.paidAmount)
        assertEquals(1, item.quantity)
        assertEquals(true, item.isClosed)
        assertEquals(createdAt, item.participatedAt)
    }

    @Test
    fun `픽업 완료 참여 내역 조회 시 픽업 완료 상태 조건 페이지 반환`() {
        val userId = 5L
        val pageable = PageRequest.of(0, 20)
        val now = LocalDateTime.now()
        val participation = ParticipationFixture.createParticipation(
            participationId = 501L,
            groupBuyId = 1001L,
            quantity = 1,
            totalAmount = 18000,
            currentQuantity = 50,
            targetQuantity = 50,
            deadline = now.minusDays(1),
            pickupDate = now.toLocalDate().minusDays(1),
            pickupTimeStart = LocalTime.of(13, 0),
            createdAt = now.minusDays(3),
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatus = PickupStatus.PICKED_UP,
            groupBuyStatus = GroupBuyStatus.COMPLETED
        ).apply {
            pickedUpAt = now.minusDays(1)
        }
        val page = PageImpl(listOf(participation), pageable, 1)

        `when`(
            participationRepository.findPickupCompletedByUserId(
                userId = userId,
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
                pickupStatuses = listOf(PickupStatus.PICKED_UP),
                pageable = pageable
            )
        ).thenReturn(page)

        val result = service.getPickupCompletedParticipations(userId, pageable)

        verify(participationRepository).findPickupCompletedByUserId(
            userId = userId,
            participationStatuses = listOf(ParticipationStatus.CONFIRMED),
            pickupStatuses = listOf(PickupStatus.PICKED_UP),
            pageable = pageable
        )
        assertEquals(1L, result.totalElements)
        assertEquals(501L, result.content.first().participationId)
        assertEquals(participation.pickedUpAt, result.content.first().pickedUpAt)
    }

    @Test
    fun `환불 취소 참여 내역 조회 시 취소 환불 상태 조건 페이지 반환`() {
        val userId = 6L
        val pageable = PageRequest.of(0, 20)
        val now = LocalDateTime.now()
        val participation = ParticipationFixture.createParticipation(
            participationId = 601L,
            groupBuyId = 1101L,
            quantity = 1,
            totalAmount = 18000,
            currentQuantity = 10,
            targetQuantity = 50,
            deadline = now.plusDays(1),
            pickupDate = now.toLocalDate().plusDays(2),
            pickupTimeStart = LocalTime.of(13, 0),
            createdAt = now.minusDays(1),
            participationStatus = ParticipationStatus.CANCELLED
        ).apply {
            cancelReason = ParticipationCancelReason.OTHER
            cancelReasonDetail = "시간이 맞지 않습니다"
            cancelledAt = now
            refundedAt = now
        }
        val page = PageImpl(listOf(participation), pageable, 1)

        `when`(
            participationRepository.findCancelledOrRefundedByUserId(
                userId = userId,
                statuses = listOf(ParticipationStatus.CANCELLED, ParticipationStatus.REFUNDED),
                pageable = pageable
            )
        ).thenReturn(page)

        val result = service.getCancelledOrRefundedParticipations(userId, pageable)

        verify(participationRepository).findCancelledOrRefundedByUserId(
            userId = userId,
            statuses = listOf(ParticipationStatus.CANCELLED, ParticipationStatus.REFUNDED),
            pageable = pageable
        )
        assertEquals(1L, result.totalElements)
        assertEquals(ParticipationStatus.CANCELLED, result.content.first().status)
        assertEquals(ParticipationCancelReason.OTHER, result.content.first().cancelReason)
    }
}
