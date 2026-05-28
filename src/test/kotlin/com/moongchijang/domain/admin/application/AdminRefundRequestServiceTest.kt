package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestApproveRequest
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestCaseFilter
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestRejectRequest
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestStatus
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestTab
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.domain.repository.PaymentOrderRepository
import com.moongchijang.domain.payment.domain.repository.PaymentRepository
import com.moongchijang.domain.refund.application.RefundRequestSyncService
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

class AdminRefundRequestServiceTest {

    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val paymentOrderRepository: PaymentOrderRepository = mock(PaymentOrderRepository::class.java)
    private val paymentRepository: PaymentRepository = mock(PaymentRepository::class.java)
    private val refundRequestSyncService: RefundRequestSyncService = mock(RefundRequestSyncService::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-28T01:00:00Z"), ZoneId.of("Asia/Seoul"))

    private val service = AdminRefundRequestService(
        participationRepository = participationRepository,
        paymentOrderRepository = paymentOrderRepository,
        paymentRepository = paymentRepository,
        refundRequestSyncService = refundRequestSyncService,
        clock = clock,
    )

    @Test
    fun `사장님 귀책 케이스 필터는 조건에 맞는 목록을 조회한다`() {
        val pageable = PageRequest.of(0, 20)
        `when`(
            participationRepository.findAdminRefundRequests(
                statuses = listOf(ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED),
                useReviewStatusFilter = false,
                reviewStatuses = listOf(OwnerRefundReviewStatus.PENDING),
                includeNullReviewStatus = false,
                caseFilter = "OWNER_FAULT_CANCEL",
                cancelReasons = listOf(com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason.OTHER),
                keyword = "테스트",
                pageable = pageable,
            )
        ).thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = service.getRefundRequests(
            tab = AdminRefundRequestTab.ALL,
            caseFilter = AdminRefundRequestCaseFilter.OWNER_FAULT_CANCEL,
            keyword = "테스트",
            pageable = pageable,
        )

        assertTrue(result.content.isEmpty())
        assertEquals(0L, result.totalElements)
    }

    @Test
    fun `환불 요청 승인 시 상태를 처리중으로 전이하고 승인 금액을 저장한다`() {
        val participation = refundParticipation(id = 101L).apply {
            ownerRefundReviewStatus = OwnerRefundReviewStatus.PENDING
            status = ParticipationStatus.REFUND_PENDING
        }
        `when`(participationRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(participation))
        `when`(paymentOrderRepository.findByUserIdAndGroupBuyId(anyLong(), anyLong())).thenReturn(null)

        val result = service.approveRefundRequest(
            requestId = 101L,
            request = AdminRefundRequestApproveRequest(refundAmount = 21_600),
        )

        assertEquals(AdminRefundRequestStatus.IN_PROGRESS, result.status)
        assertEquals(21_600, participation.approvedRefundAmount)
        assertEquals(OwnerRefundReviewStatus.APPROVED, participation.ownerRefundReviewStatus)
        verify(refundRequestSyncService).markApproved(participation, 21_600, LocalDateTime.now(clock))
    }

    @Test
    fun `환불 가능 금액을 초과한 승인 금액은 실패한다`() {
        val participation = refundParticipation(id = 102L).apply {
            ownerRefundReviewStatus = OwnerRefundReviewStatus.PENDING
        }
        `when`(participationRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(participation))

        val exception = assertThrows<CustomException> {
            service.approveRefundRequest(
                requestId = 102L,
                request = AdminRefundRequestApproveRequest(refundAmount = 30_000),
            )
        }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `이미 승인 완료된 요청을 재승인하면 실패한다`() {
        val participation = refundParticipation(id = 103L).apply {
            status = ParticipationStatus.REFUNDED
            ownerRefundReviewStatus = OwnerRefundReviewStatus.APPROVED
            refundedAt = LocalDateTime.now(clock).minusMinutes(3)
        }
        `when`(participationRepository.findByIdForUpdate(103L)).thenReturn(Optional.of(participation))

        val exception = assertThrows<CustomException> {
            service.approveRefundRequest(
                requestId = 103L,
                request = AdminRefundRequestApproveRequest(refundAmount = 21_600),
            )
        }

        assertEquals(ErrorCode.ADMIN_REFUND_REQUEST_ALREADY_PROCESSED, exception.errorCode)
    }

    @Test
    fun `환불 요청 거절 시 상태를 거절로 전이하고 사유를 저장한다`() {
        val participation = refundParticipation(id = 104L).apply {
            ownerRefundReviewStatus = OwnerRefundReviewStatus.PENDING
            approvedRefundAmount = 12_000
        }
        `when`(participationRepository.findByIdForUpdate(104L)).thenReturn(Optional.of(participation))
        `when`(paymentOrderRepository.findByUserIdAndGroupBuyId(anyLong(), anyLong())).thenReturn(null)

        val result = service.rejectRefundRequest(
            requestId = 104L,
            request = AdminRefundRequestRejectRequest(rejectionReason = " 증빙 자료 불충분 "),
        )

        assertEquals(AdminRefundRequestStatus.REJECTED, result.status)
        assertEquals(OwnerRefundReviewStatus.DISPUTED, participation.ownerRefundReviewStatus)
        assertEquals("증빙 자료 불충분", participation.ownerRefundDisputeReason)
        assertNull(participation.approvedRefundAmount)
        verify(refundRequestSyncService).markRejected(participation, "증빙 자료 불충분", LocalDateTime.now(clock))
    }

    @Test
    fun `환불 요청 거절 시 사유가 공백이면 실패한다`() {
        val exception = assertThrows<CustomException> {
            service.rejectRefundRequest(
                requestId = 105L,
                request = AdminRefundRequestRejectRequest(rejectionReason = "   "),
            )
        }
        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `거절 사유는 200자까지 저장 가능하다`() {
        val reason200 = "a".repeat(200)
        val participation = refundParticipation(id = 106L).apply {
            ownerRefundReviewStatus = OwnerRefundReviewStatus.PENDING
        }
        `when`(participationRepository.findByIdForUpdate(106L)).thenReturn(Optional.of(participation))
        `when`(paymentOrderRepository.findByUserIdAndGroupBuyId(anyLong(), anyLong())).thenReturn(null)

        val result = service.rejectRefundRequest(
            requestId = 106L,
            request = AdminRefundRequestRejectRequest(rejectionReason = reason200),
        )

        assertEquals(AdminRefundRequestStatus.REJECTED, result.status)
        assertEquals(reason200, participation.ownerRefundDisputeReason)
    }

    private fun refundParticipation(id: Long): Participation {
        val user = UserFixture.createKakaoUser(id = 7L, nickname = "환불유저")
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 3001L,
            status = GroupBuyStatus.ACHIEVED,
            productName = "초코 크루아상",
            price = 24_000,
        )
        groupBuy.store.id = 11L

        return Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 21_600,
            feeAmount = 2_400,
            totalAmount = 24_000,
            status = ParticipationStatus.REFUND_PENDING,
            cancelReasonDetail = "환불 요청",
            cancelledAt = LocalDateTime.now(clock).minusHours(2),
            id = id,
        )
    }
}
