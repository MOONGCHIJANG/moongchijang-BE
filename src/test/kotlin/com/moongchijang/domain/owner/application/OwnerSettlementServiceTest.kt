package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundRequestTab
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundReviewActionType
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundReviewSubmitRequest
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OwnerSettlementServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeStaffRepository: StoreStaffRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @InjectMocks
    private lateinit var service: OwnerSettlementService

    @Test
    fun `월별 정산 예정 금액을 조회한다`() {
        val owner = seller()
        val storeIds = defaultStoreIds()
        stubSellerAndStoreIds(owner.id!!, owner, storeIds)
        `when`(
            participationRepository.sumTotalAmountByStoreIdsAndStatusesAndYearMonth(
                storeIds,
                listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
                listOf(ParticipationStatus.CONFIRMED),
                2026,
                5,
            )
        ).thenReturn(240000L)
        `when`(
            participationRepository.sumRefundFeeAmountByStoreIdsAndYearMonth(
                storeIds,
                listOf(ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED),
                2026,
                5,
            )
        ).thenReturn(24000L)

        val result = service.getMonthlySettlementSummary(owner.id!!, 2026, 5)

        assertEquals(2026, result.year)
        assertEquals(5, result.month)
        assertEquals(240000L, result.grossRevenueAmount)
        assertEquals(24000L, result.refundFeeAmount)
        assertEquals(216000L, result.settlementExpectedAmount)
    }

    @Test
    fun `정산 월 칩을 연월 기준으로 중복 없이 반환한다`() {
        val owner = seller()
        stubSellerAndStoreIds(owner.id!!, owner, defaultStoreIds())
        `when`(
            groupBuyRepository.findDistinctPickupDatesByStoreIdsAndStatuses(
                defaultStoreIds(),
                listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
            )
        ).thenReturn(
            listOf(
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 4, 29),
            )
        )

        val result = service.getSettlementMonthChips(owner.id!!)

        assertEquals(2, result.chips.size)
        assertEquals("2026년 5월", result.chips[0].label)
        assertEquals("2026년 4월", result.chips[1].label)
    }

    @Test
    fun `환불 요청 목록 탭 필터를 적용한다`() {
        val owner = seller()
        stubSellerAndStoreIds(owner.id!!, owner, defaultStoreIds())
        `when`(
            participationRepository.findRefundRequestsByStoreIdsAndStatuses(
                defaultStoreIds(),
                listOf(ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED),
                LocalDate.now(ZoneId.of("Asia/Seoul")).minusMonths(6).atStartOfDay(),
            )
        ).thenReturn(
            listOf(
                refundParticipation(id = 1001L, status = ParticipationStatus.REFUND_PENDING),
                refundParticipation(id = 1002L, status = ParticipationStatus.REFUNDED),
            )
        )
        val pendingOnly = service.getRefundRequests(owner.id!!, OwnerRefundRequestTab.PENDING)
        val all = service.getRefundRequests(owner.id!!, OwnerRefundRequestTab.ALL)

        assertEquals(1, pendingOnly.items.size)
        assertEquals(2, all.items.size)
        assertTrue(all.hasPendingItems)
    }

    @Test
    fun `환불 요청 상세를 조회한다`() {
        val owner = seller()
        val participation = refundParticipation(id = 1010L, status = ParticipationStatus.REFUND_PENDING)
        stubSellerAndStoreIds(owner.id!!, owner, defaultStoreIds())
        `when`(participationRepository.findById(1010L)).thenReturn(Optional.of(participation))

        val detail = service.getRefundRequestDetail(owner.id!!, 1010L)

        assertEquals(1010L, detail.participationId)
        assertEquals(24000, detail.paymentAmount)
        assertEquals(2400, detail.penaltyAmount)
        assertEquals(21600, detail.refundExpectedAmount)
    }

    @Test
    fun `사장님 권한이 아니면 환불 요청 조회가 불가하다`() {
        val buyer = UserFixture.createKakaoUser(id = 99L)
        `when`(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(buyer)

        val exception = assertThrows<CustomException> {
            service.getRefundRequests(99L, OwnerRefundRequestTab.ALL)
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
        verifyNoInteractions(storeStaffRepository)
    }

    @Test
    fun `환불 요청 검토 제출 성공`() {
        val owner = seller()
        val participation = refundParticipation(id = 1020L, status = ParticipationStatus.REFUND_PENDING)
        stubSellerAndStoreIds(owner.id!!, owner, defaultStoreIds())
        `when`(participationRepository.findByIdForUpdate(1020L)).thenReturn(Optional.of(participation))

        val response = service.submitRefundReview(
            ownerId = owner.id!!,
            participationId = 1020L,
            request = OwnerRefundReviewSubmitRequest(action = OwnerRefundReviewActionType.APPROVE),
        )

        assertEquals(1020L, response.participationId)
        assertTrue(response.processed)
    }

    @Test
    fun `환불 요청 검토 제출 시 이의제기 사유 누락 예외`() {
        val owner = seller()
        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)

        val exception = assertThrows<CustomException> {
            service.submitRefundReview(
                ownerId = owner.id!!,
                participationId = 1021L,
                request = OwnerRefundReviewSubmitRequest(
                    action = OwnerRefundReviewActionType.DISPUTE,
                    disputeReason = " ",
                ),
            )
        }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `이미 처리된 환불 요청은 재처리할 수 없다`() {
        val owner = seller()
        val participation = refundParticipation(id = 1022L, status = ParticipationStatus.REFUND_PENDING).apply {
            ownerRefundReviewStatus = OwnerRefundReviewStatus.APPROVED
            ownerRefundReviewedAt = LocalDateTime.now().minusMinutes(3)
        }
        stubSellerAndStoreIds(owner.id!!, owner, defaultStoreIds())
        `when`(participationRepository.findByIdForUpdate(1022L)).thenReturn(Optional.of(participation))

        val exception = assertThrows<CustomException> {
            service.submitRefundReview(
                ownerId = owner.id!!,
                participationId = 1022L,
                request = OwnerRefundReviewSubmitRequest(action = OwnerRefundReviewActionType.APPROVE),
            )
        }

        assertEquals(ErrorCode.OWNER_REFUND_REVIEW_ALREADY_PROCESSED, exception.errorCode)
    }

    private fun seller() = UserFixture.createKakaoUser(id = 1L).apply {
        role = UserRole.SELLER
    }

    private fun defaultStoreIds(): List<Long> = listOf(1L)

    private fun stubSellerAndStoreIds(
        ownerId: Long,
        owner: com.moongchijang.domain.user.domain.entity.User,
        storeIds: List<Long>,
    ) {
        `when`(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(ownerId)).thenReturn(storeIds)
    }

    private fun refundParticipation(id: Long, status: ParticipationStatus): Participation {
        val user = UserFixture.createKakaoUser(id = 11L, nickname = "김민지")
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 901001L,
            status = GroupBuyStatus.COMPLETED,
            productName = "초코 크루아상&소금빵 세트",
            price = 24000,
        )
        groupBuy.store.id = 1L

        return Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 21600,
            feeAmount = 2400,
            totalAmount = 24000,
            status = status,
            pickupStatus = PickupStatus.NOT_READY,
            cancelReason = ParticipationCancelReason.TIME_UNAVAILABLE,
            cancelReasonDetail = "일정 변경으로 픽업이 어려워졌어요. 환불 부탁드립니다",
            cancelledAt = LocalDateTime.now().minusDays(1),
            refundedAt = if (status == ParticipationStatus.REFUNDED) LocalDateTime.now().minusHours(6) else null,
            id = id,
        ).also {
            setAuditFields(it, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1))
        }
    }

    private fun setAuditFields(entity: Any, createdAt: LocalDateTime, updatedAt: LocalDateTime) {
        val baseClass = entity.javaClass.superclass
        val createdAtField = baseClass.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(entity, createdAt)

        val updatedAtField = baseClass.getDeclaredField("updatedAt")
        updatedAtField.isAccessible = true
        updatedAtField.set(entity, updatedAt)
    }
}
