package com.moongchijang.domain.mypage.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.mypage.application.dto.MypageParticipationStatusFilter
import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import com.moongchijang.domain.payment.domain.repository.ParticipationPaymentSummary
import com.moongchijang.domain.payment.domain.repository.PaymentOrderRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class MypageServiceTest {

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    @Mock
    private lateinit var paymentOrderRepository: PaymentOrderRepository

    @InjectMocks
    private lateinit var mypageService: MypageService

    @Test
    fun `summary는 Figma 마이페이지 탭 기준 count를 반환한다`() {
        val userId = 1L
        `when`(
            participationRepository.countByUserIdAndStatusInAndPickupStatusIn(
                userId,
                listOf(ParticipationStatus.PAID_WAITING_GOAL),
                listOf(PickupStatus.NOT_READY, PickupStatus.READY)
            )
        ).thenReturn(2L)
        `when`(
            participationRepository.countByUserIdAndStatusInAndPickupStatusIn(
                userId,
                listOf(ParticipationStatus.CONFIRMED),
                listOf(PickupStatus.NOT_READY, PickupStatus.READY)
            )
        ).thenReturn(5L)
        `when`(
            participationRepository.countByUserIdAndStatusInAndPickupStatus(
                userId,
                listOf(ParticipationStatus.CONFIRMED),
                PickupStatus.PICKED_UP
            )
        ).thenReturn(3L)
        `when`(
            participationRepository.countByUserIdAndStatusIn(
                userId,
                listOf(ParticipationStatus.CANCELLED, ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED)
            )
        )
            .thenReturn(1L)
        `when`(groupBuyRequestRepository.countByUserId(userId)).thenReturn(4L)

        val result = mypageService.getSummary(userId)

        assertEquals(2L, result.inProgressCount)
        assertEquals(5L, result.pickupWaitingCount)
        assertEquals(3L, result.pickupCompletedCount)
        assertEquals(1L, result.cancelledOrRefundedCount)
        assertEquals(4L, result.requestCount)
    }

    @Test
    fun `refunds는 환불대기와 환불완료 참여를 환불 내역 DTO로 변환한다`() {
        val userId = 1L
        val pending = createParticipation().apply {
            id = 9L
            status = ParticipationStatus.REFUND_PENDING
            cancelReason = ParticipationCancelReason.TIME_UNAVAILABLE
            cancelReasonDetail = "마감 전 직접 취소"
            refundedAt = null
        }
        val completed = createParticipation().apply {
            id = 10L
            status = ParticipationStatus.REFUNDED
            cancelReason = ParticipationCancelReason.TIME_UNAVAILABLE
            cancelReasonDetail = "마감 전 직접 취소"
            refundedAt = LocalDateTime.of(2026, 5, 20, 10, 0)
        }
        `when`(
            participationRepository.findByUserIdAndStatusInOrderByRefundedAtDescCreatedAtDesc(
                userId,
                listOf(ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED),
                ParticipationStatus.REFUND_PENDING
            )
        ).thenReturn(listOf(pending, completed))
        `when`(
            paymentOrderRepository.findPaymentSummariesByParticipationIdIn(listOf(9L, 10L))
        ).thenReturn(
            listOf(
                paymentSummary(
                    participationId = 9L,
                    groupBuyId = 100L,
                    orderStatus = PaymentOrderStatus.APPROVED,
                    paidAt = LocalDateTime.of(2026, 5, 18, 9, 0),
                    paymentMethod = "CARD"
                ),
                paymentSummary(
                    participationId = 10L,
                    groupBuyId = 100L,
                    orderStatus = PaymentOrderStatus.APPROVED,
                    paidAt = LocalDateTime.of(2026, 5, 19, 9, 0),
                    paymentMethod = "KAKAOPAY"
                )
            )
        )

        val result = mypageService.getRefunds(userId)

        assertEquals(2, result.size)
        assertEquals(9L, result[0].participationId)
        assertEquals("https://example.com/cake.jpg", result[0].thumbnailUrl)
        assertEquals("초코 케이크", result[0].productName)
        assertEquals("PENDING", result[0].refundStatus)
        assertEquals("문치 베이커리", result[0].storeName)
        assertEquals(LocalDate.of(2026, 5, 25), result[0].pickupDate)
        assertEquals(LocalTime.of(13, 0), result[0].pickupTimeStart)
        assertEquals(LocalTime.of(15, 0), result[0].pickupTimeEnd)
        assertEquals(24_000, result[0].paymentAmount)
        assertEquals(2, result[0].quantity)
        assertEquals(ParticipationCancelReason.TIME_UNAVAILABLE.name, result[0].cancelReason)
        assertEquals("마감 전 직접 취소", result[0].cancelReasonDetail)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0), result[0].paidAt)
        assertEquals("CARD", result[0].paymentMethod)
        assertEquals(null, result[0].refundedAt)
        assertEquals(10L, result[1].participationId)
        assertEquals("https://example.com/cake.jpg", result[1].thumbnailUrl)
        assertEquals("COMPLETED", result[1].refundStatus)
        assertEquals(LocalDateTime.of(2026, 5, 19, 9, 0), result[1].paidAt)
        assertEquals("KAKAOPAY", result[1].paymentMethod)
        assertEquals(LocalDateTime.of(2026, 5, 20, 10, 0), result[1].refundedAt)
    }

    @Test
    fun `in-progress participations는 진행 중 조건으로 DTO를 반환한다`() {
        val userId = 1L
        val participation = createParticipation().apply {
            id = 11L
            groupBuy.id = 101L
            groupBuy.currentQuantity = 6
            status = ParticipationStatus.PAID_WAITING_GOAL
            pickupStatus = PickupStatus.NOT_READY
        }
        `when`(
            participationRepository.findByUserIdAndStatusInAndPickupStatusInOrderByCreatedAtDesc(
                userId,
                listOf(ParticipationStatus.PAID_WAITING_GOAL),
                listOf(PickupStatus.NOT_READY, PickupStatus.READY)
            )
        ).thenReturn(listOf(participation))
        `when`(
            paymentOrderRepository.findPaymentSummariesByParticipationIdIn(listOf(11L))
        ).thenReturn(
            listOf(
                paymentSummary(
                    participationId = 11L,
                    groupBuyId = 101L,
                    orderStatus = PaymentOrderStatus.CANCELLED,
                    paidAt = LocalDateTime.of(2026, 5, 23, 8, 0),
                    paymentMethod = "KAKAOPAY"
                ),
                paymentSummary(
                    participationId = 11L,
                    groupBuyId = 101L,
                    orderStatus = PaymentOrderStatus.APPROVED,
                    paidAt = LocalDateTime.of(2026, 5, 24, 9, 30),
                    paymentMethod = "CARD"
                )
            )
        )

        val result = mypageService.getParticipations(userId, MypageParticipationStatusFilter.IN_PROGRESS)

        assertEquals(1, result.size)
        assertEquals(11L, result[0].participationId)
        assertEquals(101L, result[0].groupBuyId)
        assertEquals("https://example.com/cake.jpg", result[0].thumbnailUrl)
        assertEquals("초코 케이크", result[0].productName)
        assertEquals(ParticipationStatus.PAID_WAITING_GOAL.name, result[0].participationStatus)
        assertEquals(60, result[0].achievementRate)
        assertEquals("BEFORE_ACHIEVED", result[0].achievementStatus)
        assertEquals("PAID_WAITING_GOAL", result[0].displayStatus)
        assertEquals("문치 베이커리", result[0].storeName)
        assertEquals(LocalDate.of(2026, 5, 25), result[0].pickupDate)
        assertEquals(LocalTime.of(13, 0), result[0].pickupTimeStart)
        assertEquals(LocalTime.of(15, 0), result[0].pickupTimeEnd)
        assertEquals("문치 베이커리", result[0].pickupLocation)
        assertEquals(24_000, result[0].paymentAmount)
        assertEquals(LocalDateTime.of(2026, 5, 24, 9, 30), result[0].paidAt)
        assertEquals("CARD", result[0].paymentMethod)
        assertEquals(2, result[0].quantity)
        assertEquals(PickupStatus.NOT_READY.name, result[0].pickupStatus)
        assertEquals(1, result[0].dDay)
        assertEquals(true, result[0].canCancel)
        assertEquals(false, result[0].canViewPickup)
        assertEquals(false, result[0].canViewQr)
        assertEquals("UNAVAILABLE", result[0].qrAvailability)
    }

    @Test
    fun `in-progress participations가 비어 있으면 결제 주문을 조회하지 않는다`() {
        val userId = 1L
        `when`(
            participationRepository.findByUserIdAndStatusInAndPickupStatusInOrderByCreatedAtDesc(
                userId,
                listOf(ParticipationStatus.PAID_WAITING_GOAL),
                listOf(PickupStatus.NOT_READY, PickupStatus.READY)
            )
        ).thenReturn(emptyList())

        val result = mypageService.getParticipations(userId, MypageParticipationStatusFilter.IN_PROGRESS)

        assertEquals(emptyList<Any>(), result)
        verify(paymentOrderRepository, never()).findPaymentSummariesByParticipationIdIn(emptyList())
    }

    @Test
    fun `pickup-completed participations는 픽업 완료 조건으로 DTO를 반환한다`() {
        val userId = 1L
        val participation = createParticipation().apply {
            id = 12L
            groupBuy.id = 102L
            status = ParticipationStatus.CONFIRMED
            pickupStatus = PickupStatus.PICKED_UP
            pickedUpAt = LocalDateTime.of(2026, 5, 25, 14, 0)
        }
        `when`(
            participationRepository.findByUserIdAndStatusInAndPickupStatusOrderByPickedUpAtDescCreatedAtDesc(
                userId,
                listOf(ParticipationStatus.CONFIRMED),
                PickupStatus.PICKED_UP
            )
        ).thenReturn(listOf(participation))
        `when`(
            paymentOrderRepository.findPaymentSummariesByParticipationIdIn(listOf(12L))
        ).thenReturn(
            listOf(
                paymentSummary(
                    participationId = 12L,
                    groupBuyId = 102L,
                    orderStatus = PaymentOrderStatus.CANCELLED,
                    paidAt = LocalDateTime.of(2026, 5, 23, 8, 0),
                    paymentMethod = "KAKAOPAY"
                )
            )
        )

        val result = mypageService.getParticipations(userId, MypageParticipationStatusFilter.PICKUP_COMPLETED)

        assertEquals(1, result.size)
        assertEquals(12L, result[0].participationId)
        assertEquals(102L, result[0].groupBuyId)
        assertEquals("https://example.com/cake.jpg", result[0].thumbnailUrl)
        assertEquals("초코 케이크", result[0].productName)
        assertEquals(ParticipationStatus.CONFIRMED.name, result[0].participationStatus)
        assertEquals(0, result[0].achievementRate)
        assertEquals("ACHIEVED", result[0].achievementStatus)
        assertEquals("PICKED_UP", result[0].displayStatus)
        assertEquals("문치 베이커리", result[0].storeName)
        assertEquals(LocalDate.of(2026, 5, 25), result[0].pickupDate)
        assertEquals(LocalTime.of(13, 0), result[0].pickupTimeStart)
        assertEquals(LocalTime.of(15, 0), result[0].pickupTimeEnd)
        assertEquals(24_000, result[0].paymentAmount)
        assertEquals(LocalDateTime.of(2026, 5, 23, 8, 0), result[0].paidAt)
        assertEquals("KAKAOPAY", result[0].paymentMethod)
        assertEquals(2, result[0].quantity)
        assertEquals(PickupStatus.PICKED_UP.name, result[0].pickupStatus)
        assertEquals(false, result[0].canCancel)
        assertEquals(false, result[0].canViewPickup)
        assertEquals(false, result[0].canViewQr)
        assertEquals("PICKED_UP", result[0].qrAvailability)
    }

    @Test
    fun `cancelled-or-refunded participations는 취소 환불 상태 목록 DTO를 반환한다`() {
        val userId = 1L
        val participation = createParticipation().apply {
            id = 13L
            groupBuy.id = 103L
            status = ParticipationStatus.CANCELLED
        }
        `when`(
            participationRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                userId,
                listOf(ParticipationStatus.CANCELLED, ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED)
            )
        ).thenReturn(listOf(participation))
        `when`(
            paymentOrderRepository.findPaymentSummariesByParticipationIdIn(listOf(13L))
        ).thenReturn(emptyList())

        val result = mypageService.getParticipations(userId, MypageParticipationStatusFilter.CANCELLED_OR_REFUNDED)

        assertEquals(1, result.size)
        assertEquals(13L, result[0].participationId)
        assertEquals(ParticipationStatus.CANCELLED.name, result[0].participationStatus)
        assertEquals("CANCELLED", result[0].displayStatus)
        assertEquals(false, result[0].canCancel)
    }

    @Test
    fun `group-buy-requests는 내 개설 요청 내역 DTO로 변환한다`() {
        val userId = 1L
        val request = GroupBuyRequest(
            userId = userId,
            storeName = "문치 베이커리",
            productName = "소금빵",
            desiredQuantity = 5,
            desiredPickupDate = LocalDate.of(2026, 5, 27),
            status = GroupBuyRequestStatus.IN_CONTACT
        ).apply { id = 20L }
        `when`(groupBuyRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(listOf(request))

        val result = mypageService.getGroupBuyRequests(userId)

        assertEquals(1, result.size)
        assertEquals(20L, result[0].requestId)
        assertEquals("소금빵", result[0].productName)
        assertEquals(GroupBuyRequestStatus.IN_CONTACT.name, result[0].status)
        assertEquals("문치 베이커리", result[0].storeName)
        assertEquals(LocalDate.of(2026, 5, 27), result[0].desiredPickupDate)
        assertEquals(5, result[0].desiredQuantity)

        verify(groupBuyRequestRepository).findByUserIdOrderByCreatedAtDesc(userId)
    }

    private fun createParticipation(): Participation {
        val user = User(
            provider = AuthProvider.EMAIL,
            email = "user@example.com",
            passwordHash = "password",
            nickname = "문치",
            role = UserRole.BUYER
        ).apply { id = 1L }
        val store = Store(
            name = "문치 베이커리",
            address = "서울시 성동구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN
        )
        val groupBuyRequest = GroupBuyRequest(
            userId = 2L,
            storeName = store.name,
            productName = "초코 케이크",
            desiredQuantity = 10,
            desiredPickupDate = LocalDate.of(2026, 5, 25)
        )
        val groupBuy = com.moongchijang.domain.groupbuy.domain.entity.GroupBuy(
            store = store,
            groupBuyRequest = groupBuyRequest,
            thumbnailKey = "https://example.com/cake.jpg",
            productName = "초코 케이크",
            productDescription = "진한 초코 케이크",
            price = 12_000,
            targetQuantity = 10,
            maxQuantity = 20,
            status = com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus.IN_PROGRESS,
            deadline = LocalDate.now().plusDays(1).atTime(23, 59),
            pickupDate = LocalDate.of(2026, 5, 25),
            pickupTimeStart = LocalTime.of(13, 0),
            pickupTimeEnd = LocalTime.of(15, 0),
            pickupLocation = "문치 베이커리"
        )

        return Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 2,
            productAmount = 24_000,
            feeAmount = 0,
            totalAmount = 24_000,
            status = ParticipationStatus.REFUNDED,
            pickupStatus = PickupStatus.NOT_READY
        )
    }

    private fun paymentSummary(
        participationId: Long,
        groupBuyId: Long,
        orderStatus: PaymentOrderStatus,
        paidAt: LocalDateTime?,
        paymentMethod: String?
    ): ParticipationPaymentSummary =
        object : ParticipationPaymentSummary {
            override val participationId: Long = participationId
            override val groupBuyId: Long = groupBuyId
            override val orderStatus: PaymentOrderStatus = orderStatus
            override val paidAt: LocalDateTime? = paidAt
            override val paymentMethod: String? = paymentMethod
        }
}
