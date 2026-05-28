package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyCloseReason
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyClosedByType
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyCloseReasonType
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyCloseRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyExtensionRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageFilterType
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.domain.repository.PaymentRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OwnerGroupBuyServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeStaffRepository: StoreStaffRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var ownerGroupBuyRequestRepository: OwnerGroupBuyRequestRepository

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @InjectMocks
    private lateinit var service: OwnerGroupBuyService

    @Test
    fun `사장님 본인 매장의 공구 목록을 조회한다`() {
        val owner = seller()
        val groupBuy = groupBuy(
            id = 10L,
            status = GroupBuyStatus.IN_PROGRESS,
            currentQuantity = 12,
            targetQuantity = 20,
            price = 9900
        )

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(listOf(1L, 2L))
        `when`(
            groupBuyRepository.findByStoreIdInAndStatusInOrderByDeadlineAsc(
                listOf(1L, 2L),
                listOf(
                    GroupBuyStatus.IN_PROGRESS,
                    GroupBuyStatus.ACHIEVED,
                    GroupBuyStatus.COMPLETED,
                    GroupBuyStatus.FAILED,
                    GroupBuyStatus.CLOSED
                )
            )
        ).thenReturn(listOf(groupBuy))

        val result = service.getMyGroupBuys(owner.id!!)

        assertEquals(1, result.size)
        assertEquals(10L, result[0].groupBuyId)
        assertEquals("두쫀쿠 1개", result[0].productName)
        assertEquals(20, result[0].targetQuantity)
        assertEquals(12, result[0].currentQuantity)
        assertEquals(60, result[0].achievementRate)
        assertEquals(9900, result[0].price)
        assertEquals(LocalDate.of(2026, 6, 1), result[0].deadline)
        assertEquals("IN_PROGRESS", result[0].status.name)
    }

    @Test
    fun `마감 이후 완료와 종료 상태도 사장님 공구 목록에 포함한다`() {
        val owner = seller()
        val completed = groupBuy(
            id = 11L,
            status = GroupBuyStatus.COMPLETED,
            currentQuantity = 20,
            targetQuantity = 20,
            price = 9900
        )
        val closed = groupBuy(
            id = 12L,
            status = GroupBuyStatus.CLOSED,
            currentQuantity = 8,
            targetQuantity = 20,
            price = 9900
        )

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(listOf(1L))
        `when`(
            groupBuyRepository.findByStoreIdInAndStatusInOrderByDeadlineAsc(
                listOf(1L),
                listOf(
                    GroupBuyStatus.IN_PROGRESS,
                    GroupBuyStatus.ACHIEVED,
                    GroupBuyStatus.COMPLETED,
                    GroupBuyStatus.FAILED,
                    GroupBuyStatus.CLOSED
                )
            )
        ).thenReturn(listOf(completed, closed))

        val result = service.getMyGroupBuys(owner.id!!)

        assertEquals(listOf("ACHIEVED", "FAILED"), result.map { it.status.name })
    }

    @Test
    fun `소속 매장이 없으면 빈 목록을 반환한다`() {
        val owner = seller()

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(emptyList())

        val result = service.getMyGroupBuys(owner.id!!)

        assertEquals(emptyList<Any>(), result)
        verifyNoInteractions(groupBuyRepository)
    }

    @Test
    fun `구매자 계정이면 사장님 공구 목록을 조회할 수 없다`() {
        val buyer = UserFixture.createKakaoUser(id = 1L)
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(buyer)

        val ex = assertThrows<CustomException> {
            service.getMyGroupBuys(1L)
        }

        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
        verify(storeStaffRepository, never()).findStoreIdsByUserId(1L)
    }

    @Test
    fun `소속 매장이 없으면 공구 요약 빈 상태를 반환`() {
        val owner = seller()
        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(emptyList())

        val result = service.getMyGroupBuySummary(owner.id!!)

        assertEquals(0, result.ongoingCount)
        assertEquals(0, result.achievedCount)
        assertEquals(0, result.todayPickupUserCount)
        assertEquals(0L, result.settlementExpectedAmount)
        assertEquals(true, result.isEmpty)
        verifyNoInteractions(groupBuyRepository)
    }

    @Test
    fun `사장님 공구 요약 정상 집계`() {
        val owner = seller()
        val storeIds = listOf(1L, 2L)

        mockSellerAndStoreIds(owner.id!!, owner, storeIds)
        mockSummaryCounts(storeIds, ongoing = 3L, achieved = 2L, todayPickupUsers = 14L, settlementAmount = 128000L)

        val result = service.getMyGroupBuySummary(owner.id!!)

        assertEquals(3, result.ongoingCount)
        assertEquals(2, result.achievedCount)
        assertEquals(14, result.todayPickupUserCount)
        assertEquals(128000L, result.settlementExpectedAmount)
        assertEquals(false, result.isEmpty)
    }

    @Test
    fun `구매자 계정이면 사장님 공구 요약 조회 불가`() {
        val buyer = UserFixture.createKakaoUser(id = 1L)
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(buyer)

        val ex = assertThrows<CustomException> {
            service.getMyGroupBuySummary(1L)
        }

        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
        verify(storeStaffRepository, never()).findStoreIdsByUserId(1L)
    }

    @Test
    fun `사장님 공구 관리 승인대기 목록 조회`() {
        val owner = seller()
        val storeIds = listOf(1L)
        mockSellerAndStoreIds(owner.id!!, owner, storeIds)
        `when`(
            ownerGroupBuyRequestRepository.findByOwnerIdAndStoreIdInAndStatusOrderByCreatedAtDesc(
                owner.id!!,
                storeIds,
                OwnerGroupBuyRequestStatus.PENDING
            )
        ).thenReturn(emptyList())

        val result = service.getManageGroupBuys(owner.id!!, OwnerGroupBuyManageFilterType.PENDING_APPROVAL)

        assertEquals(0, result.size)
    }

    @Test
    fun `사장님 공구 관리 상세 조회 시 소속 매장이 없으면 권한 예외`() {
        val owner = seller()
        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(emptyList())

        val ex = assertThrows<CustomException> {
            service.getInProgressGroupBuyDetail(owner.id!!, 10L)
        }

        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `사장님 공구 기간 연장 요청 성공`() {
        val owner = seller()
        val groupBuy = groupBuy(
            id = 10L,
            status = GroupBuyStatus.IN_PROGRESS,
            currentQuantity = 12,
            targetQuantity = 20,
            price = 9900
        )
        groupBuy.pickupDate = groupBuy.deadline.toLocalDate().plusDays(3)
        val request = OwnerGroupBuyExtensionRequest(extendedDeadline = groupBuy.deadline.plusDays(2))

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(listOf(1L))
        `when`(groupBuyRepository.findWithStoreById(groupBuy.id)).thenReturn(Optional.of(groupBuy))

        service.requestGroupBuyExtension(owner.id!!, groupBuy.id, request)

        assertEquals(request.extendedDeadline, groupBuy.deadline)
        verify(groupBuyRepository).save(groupBuy)
    }

    @Test
    fun `사장님 공구 기간 연장 요청 시 기존 마감 이전 날짜는 실패`() {
        val owner = seller()
        val groupBuy = groupBuy(
            id = 11L,
            status = GroupBuyStatus.IN_PROGRESS,
            currentQuantity = 12,
            targetQuantity = 20,
            price = 9900
        )
        val request = OwnerGroupBuyExtensionRequest(extendedDeadline = groupBuy.deadline.minusDays(1))

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(listOf(1L))
        `when`(groupBuyRepository.findWithStoreById(groupBuy.id)).thenReturn(Optional.of(groupBuy))

        val ex = assertThrows<CustomException> {
            service.requestGroupBuyExtension(owner.id!!, groupBuy.id, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT, ex.errorCode)
    }

    @Test
    fun `사장님 공구 마감 요청 성공`() {
        val owner = seller()
        val groupBuy = groupBuy(
            id = 12L,
            status = GroupBuyStatus.ACHIEVED,
            currentQuantity = 20,
            targetQuantity = 20,
            price = 9900
        )
        val request = OwnerGroupBuyCloseRequest(reason = OwnerGroupBuyCloseReasonType.STORE_CONDITION)

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(listOf(1L))
        `when`(groupBuyRepository.findWithStoreById(groupBuy.id)).thenReturn(Optional.of(groupBuy))

        service.requestGroupBuyClose(owner.id!!, groupBuy.id, request)

        assertEquals(GroupBuyStatus.CLOSED, groupBuy.status)
        assertEquals(GroupBuyCloseReason.STORE_CONDITION, groupBuy.closeReason)
        assertEquals(GroupBuyClosedByType.OWNER, groupBuy.closedByType)
        assertNull(groupBuy.closeReasonDetail)
        verify(groupBuyRepository).save(groupBuy)
    }

    @Test
    fun `사장님 공구 마감 요청에서 기타 사유 상세 누락 시 실패`() {
        val owner = seller()
        val groupBuy = groupBuy(
            id = 13L,
            status = GroupBuyStatus.IN_PROGRESS,
            currentQuantity = 12,
            targetQuantity = 20,
            price = 9900
        )
        val request = OwnerGroupBuyCloseRequest(
            reason = OwnerGroupBuyCloseReasonType.OTHER,
            reasonDetail = "   "
        )

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(listOf(1L))
        `when`(groupBuyRepository.findWithStoreById(groupBuy.id)).thenReturn(Optional.of(groupBuy))

        val ex = assertThrows<CustomException> {
            service.requestGroupBuyClose(owner.id!!, groupBuy.id, request)
        }

        assertEquals(ErrorCode.INVALID_INPUT, ex.errorCode)
    }

    private fun seller() = UserFixture.createKakaoUser(id = 1L).apply {
        role = UserRole.SELLER
    }

    private fun mockSellerAndStoreIds(ownerId: Long, owner: com.moongchijang.domain.user.domain.entity.User, storeIds: List<Long>) {
        `when`(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(ownerId)).thenReturn(storeIds)
    }

    private fun mockSummaryCounts(
        storeIds: List<Long>,
        ongoing: Long,
        achieved: Long,
        todayPickupUsers: Long,
        settlementAmount: Long
    ) {
        `when`(groupBuyRepository.countByStoreIdsAndStatuses(storeIds, listOf(GroupBuyStatus.IN_PROGRESS))).thenReturn(ongoing)
        `when`(
            groupBuyRepository.countByStoreIdsAndStatuses(
                storeIds,
                listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
            )
        ).thenReturn(achieved)
        `when`(
            groupBuyRepository.countTodayPickupUsersByStoreIds(
                storeIds = storeIds,
                pickupDate = LocalDate.now(ZoneId.of("Asia/Seoul")),
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
                pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
            )
        ).thenReturn(todayPickupUsers)
        `when`(
            groupBuyRepository.sumSettlementExpectedAmountByStoreIds(
                storeIds = storeIds,
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
                groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
            )
        ).thenReturn(settlementAmount)
    }

    private fun groupBuy(
        id: Long,
        status: GroupBuyStatus,
        currentQuantity: Int,
        targetQuantity: Int,
        price: Int
    ): GroupBuy =
        GroupBuyFixture.createGroupBuy(
            id = id,
            status = status,
            deadline = LocalDateTime.of(2026, 6, 1, 23, 59),
            currentQuantity = currentQuantity,
            targetQuantity = targetQuantity,
            price = price
        )
}
