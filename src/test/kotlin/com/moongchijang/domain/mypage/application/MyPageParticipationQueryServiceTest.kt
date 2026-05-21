package com.moongchijang.domain.mypage.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.support.UserFixture
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

    private val service by lazy {
        MyPageParticipationQueryService(participationRepository)
    }

    @Test
    fun `진행 중 참여 내역 조회 시 진행 중 상태 조건 페이지 반환`() {
        val userId = 1L
        val pageable = PageRequest.of(0, 20)
        val now = LocalDateTime.now()
        val participation = createParticipation(
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
                statuses = listOf(ParticipationStatus.PAID_WAITING_GOAL, ParticipationStatus.CONFIRMED),
                pageable = pageable
            )
        ).thenReturn(page)

        val result = service.getInProgressParticipations(userId, pageable)

        verify(participationRepository).findInProgressByUserId(
            userId = userId,
            statuses = listOf(
                ParticipationStatus.PAID_WAITING_GOAL,
                ParticipationStatus.CONFIRMED
            ),
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

        val participation = createParticipation(
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
                statuses = listOf(ParticipationStatus.PAID_WAITING_GOAL, ParticipationStatus.CONFIRMED),
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
        val participation = createParticipation(
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

        val participation = createParticipation(
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

    private fun createParticipation(
        participationId: Long,
        groupBuyId: Long,
        quantity: Int,
        totalAmount: Int,
        currentQuantity: Int,
        targetQuantity: Int,
        deadline: LocalDateTime,
        pickupDate: LocalDate,
        pickupTimeStart: LocalTime,
        createdAt: LocalDateTime,
        participationStatus: ParticipationStatus = ParticipationStatus.PAID_WAITING_GOAL,
        pickupStatus: PickupStatus = PickupStatus.NOT_READY,
        groupBuyStatus: GroupBuyStatus = GroupBuyStatus.IN_PROGRESS
    ): Participation {
        val user = UserFixture.createKakaoUser(id = 1L, nickname = "테스터")
        val groupBuy = createGroupBuy(
            groupBuyId = groupBuyId,
            currentQuantity = currentQuantity,
            targetQuantity = targetQuantity,
            deadline = deadline,
            pickupDate = pickupDate,
            pickupTimeStart = pickupTimeStart,
            groupBuyStatus = groupBuyStatus
        )

        return Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = quantity,
            productAmount = totalAmount,
            feeAmount = 0,
            totalAmount = totalAmount,
            status = participationStatus,
            pickupStatus = pickupStatus
        ).apply {
            id = participationId
            setCreatedAt(this, createdAt)
        }
    }

    private fun createGroupBuy(
        groupBuyId: Long,
        currentQuantity: Int,
        targetQuantity: Int,
        deadline: LocalDateTime,
        pickupDate: LocalDate,
        pickupTimeStart: LocalTime,
        groupBuyStatus: GroupBuyStatus
    ): GroupBuy {
        val store = Store(
            name = "사이드템포",
            address = "서울 강남구 OO길 1",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG
        )
        val request = GroupBuyRequest(
            userId = 1L,
            storeName = "사이드템포",
            productName = "두쫀쿠 오리지널 1개",
            desiredQuantity = 50,
            desiredPickupDate = pickupDate
        )
        return GroupBuy(
            store = store,
            groupBuyRequest = request,
            productName = "두쫀쿠 오리지널 1개",
            productDescription = "테스트 상품 설명",
            price = 18000,
            targetQuantity = targetQuantity,
            currentQuantity = currentQuantity,
            maxQuantity = 100,
            status = groupBuyStatus,
            deadline = deadline,
            pickupDate = pickupDate,
            pickupTimeStart = pickupTimeStart,
            pickupTimeEnd = pickupTimeStart.plusHours(4),
            pickupLocation = "매장 앞"
        ).apply {
            id = groupBuyId
        }
    }

    private fun setCreatedAt(participation: Participation, createdAt: LocalDateTime) {
        val field = participation.javaClass.superclass.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(participation, createdAt)
    }
}
