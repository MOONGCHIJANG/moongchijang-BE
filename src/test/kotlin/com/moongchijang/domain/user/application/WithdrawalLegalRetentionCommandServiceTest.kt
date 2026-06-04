package com.moongchijang.domain.user.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.domain.entity.PaymentOrder
import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import com.moongchijang.domain.payment.domain.repository.PaymentOrderRepository
import com.moongchijang.domain.refund.domain.entity.RefundRequest
import com.moongchijang.domain.refund.domain.entity.RefundRequestStatus
import com.moongchijang.domain.refund.domain.repository.RefundRequestRepository
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.WithdrawnAccount
import com.moongchijang.domain.user.domain.entity.WithdrawnParticipation
import com.moongchijang.domain.user.domain.entity.WithdrawnPaymentOrder
import com.moongchijang.domain.user.domain.entity.WithdrawnRefundRequest
import com.moongchijang.domain.user.domain.repository.WithdrawnAccountRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnParticipationRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnPaymentOrderRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnRefundRequestRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDateTime

class WithdrawalLegalRetentionCommandServiceTest {

    private val withdrawnAccountRepository: WithdrawnAccountRepository = Mockito.mock(WithdrawnAccountRepository::class.java)
    private val paymentOrderRepository: PaymentOrderRepository = Mockito.mock(PaymentOrderRepository::class.java)
    private val participationRepository: ParticipationRepository = Mockito.mock(ParticipationRepository::class.java)
    private val refundRequestRepository: RefundRequestRepository = Mockito.mock(RefundRequestRepository::class.java)
    private val withdrawnPaymentOrderRepository: WithdrawnPaymentOrderRepository =
        Mockito.mock(WithdrawnPaymentOrderRepository::class.java)
    private val withdrawnParticipationRepository: WithdrawnParticipationRepository =
        Mockito.mock(WithdrawnParticipationRepository::class.java)
    private val withdrawnRefundRequestRepository: WithdrawnRefundRequestRepository =
        Mockito.mock(WithdrawnRefundRequestRepository::class.java)

    private val service = WithdrawalLegalRetentionCommandService(
        withdrawnAccountRepository = withdrawnAccountRepository,
        paymentOrderRepository = paymentOrderRepository,
        participationRepository = participationRepository,
        refundRequestRepository = refundRequestRepository,
        withdrawnPaymentOrderRepository = withdrawnPaymentOrderRepository,
        withdrawnParticipationRepository = withdrawnParticipationRepository,
        withdrawnRefundRequestRepository = withdrawnRefundRequestRepository,
    )

    @Test
    fun `법정보존 대상 결제 참여 환불 데이터를 스냅샷 저장한다`() {
        val userId = 1L
        val withdrawnAt = LocalDateTime.of(2026, 6, 4, 10, 0)
        val withdrawnAccount = WithdrawnAccount(
            provider = AuthProvider.KAKAO,
            identifierHash = "hashed-id",
            withdrawnUserId = userId,
            withdrawnAt = withdrawnAt,
            rejoinAvailableAt = withdrawnAt.plusDays(30),
            id = 99L,
        )

        val groupBuy = Mockito.mock(GroupBuy::class.java)
        Mockito.`when`(groupBuy.id).thenReturn(20L)

        val paymentOrder = Mockito.mock(PaymentOrder::class.java)
        Mockito.`when`(paymentOrder.id).thenReturn(10L)
        Mockito.`when`(paymentOrder.groupBuy).thenReturn(groupBuy)
        Mockito.`when`(paymentOrder.orderId).thenReturn("order-1")
        Mockito.`when`(paymentOrder.quantity).thenReturn(2)
        Mockito.`when`(paymentOrder.productAmount).thenReturn(10000)
        Mockito.`when`(paymentOrder.feeAmount).thenReturn(500)
        Mockito.`when`(paymentOrder.totalAmount).thenReturn(10500)
        Mockito.`when`(paymentOrder.agreedNoCancelAfterGoal).thenReturn(true)
        Mockito.`when`(paymentOrder.agreedRefundBeforeGoal).thenReturn(true)
        Mockito.`when`(paymentOrder.agreedNoRefundAfterNoShow).thenReturn(false)
        Mockito.`when`(paymentOrder.agreedNoWithdrawal).thenReturn(true)
        Mockito.`when`(paymentOrder.status).thenReturn(PaymentOrderStatus.APPROVED)
        Mockito.`when`(paymentOrder.approvedAt).thenReturn(withdrawnAt.minusDays(1))
        Mockito.`when`(paymentOrder.failedAt).thenReturn(null)
        Mockito.`when`(paymentOrder.cancelledAt).thenReturn(null)

        val participation = Mockito.mock(Participation::class.java)
        Mockito.`when`(participation.id).thenReturn(30L)
        Mockito.`when`(participation.groupBuy).thenReturn(groupBuy)
        Mockito.`when`(participation.quantity).thenReturn(2)
        Mockito.`when`(participation.productAmount).thenReturn(10000)
        Mockito.`when`(participation.feeAmount).thenReturn(500)
        Mockito.`when`(participation.totalAmount).thenReturn(10500)
        Mockito.`when`(participation.status).thenReturn(ParticipationStatus.CONFIRMED)
        Mockito.`when`(participation.pickupStatus).thenReturn(PickupStatus.READY)
        Mockito.`when`(participation.pickupToken).thenReturn("pickup-token")
        Mockito.`when`(participation.pickedUpAt).thenReturn(null)
        Mockito.`when`(participation.cancelReason).thenReturn(null)
        Mockito.`when`(participation.cancelReasonDetail).thenReturn(null)
        Mockito.`when`(participation.cancelledAt).thenReturn(null)
        Mockito.`when`(participation.refundedAt).thenReturn(null)
        Mockito.`when`(participation.approvedRefundAmount).thenReturn(null)
        Mockito.`when`(participation.ownerRefundReviewStatus).thenReturn(null)
        Mockito.`when`(participation.ownerRefundDisputeReason).thenReturn(null)
        Mockito.`when`(participation.ownerRefundReviewedAt).thenReturn(null)

        val refundRequest = Mockito.mock(RefundRequest::class.java)
        Mockito.`when`(refundRequest.id).thenReturn(40L)
        Mockito.`when`(refundRequest.participation).thenReturn(participation)
        Mockito.`when`(refundRequest.status).thenReturn(RefundRequestStatus.REQUESTED)
        Mockito.`when`(refundRequest.requestedAmount).thenReturn(10500)
        Mockito.`when`(refundRequest.approvedRefundAmount).thenReturn(null)
        Mockito.`when`(refundRequest.rejectedReason).thenReturn(null)
        Mockito.`when`(refundRequest.requestedAt).thenReturn(withdrawnAt.minusHours(2))
        Mockito.`when`(refundRequest.approvedAt).thenReturn(null)
        Mockito.`when`(refundRequest.rejectedAt).thenReturn(null)
        Mockito.`when`(refundRequest.refundedAt).thenReturn(null)

        Mockito.`when`(withdrawnAccountRepository.findByWithdrawnUserId(userId)).thenReturn(withdrawnAccount)
        Mockito.`when`(paymentOrderRepository.findAllByUserId(userId)).thenReturn(listOf(paymentOrder))
        Mockito.`when`(participationRepository.findAllByUserId(userId)).thenReturn(listOf(participation))
        Mockito.`when`(refundRequestRepository.findAllByParticipationUserId(userId)).thenReturn(listOf(refundRequest))

        service.retainForWithdrawal(userId, withdrawnAt)

        val paymentCaptor: ArgumentCaptor<List<WithdrawnPaymentOrder>> = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<WithdrawnPaymentOrder>>
        Mockito.verify(withdrawnPaymentOrderRepository).saveAll(paymentCaptor.capture())
        assertEquals(1, paymentCaptor.value.size)
        assertEquals(99L, paymentCaptor.value.first().withdrawnAccountId)
        assertEquals(10L, paymentCaptor.value.first().originalPaymentOrderId)
        assertEquals(withdrawnAt.plusYears(5), paymentCaptor.value.first().retentionExpiresAt)

        val participationCaptor: ArgumentCaptor<List<WithdrawnParticipation>> = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<WithdrawnParticipation>>
        Mockito.verify(withdrawnParticipationRepository).saveAll(participationCaptor.capture())
        assertEquals(1, participationCaptor.value.size)
        assertEquals(10L, participationCaptor.value.first().originalPaymentOrderId)
        assertEquals("pickup-token", participationCaptor.value.first().pickupToken)

        val refundCaptor: ArgumentCaptor<List<WithdrawnRefundRequest>> = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<WithdrawnRefundRequest>>
        Mockito.verify(withdrawnRefundRequestRepository).saveAll(refundCaptor.capture())
        assertEquals(1, refundCaptor.value.size)
        assertEquals(40L, refundCaptor.value.first().originalRefundRequestId)
        assertNull(refundCaptor.value.first().approvedRefundAmount)
    }

    @Test
    fun `탈퇴 계정 정보가 없으면 예외`() {
        Mockito.`when`(withdrawnAccountRepository.findByWithdrawnUserId(1L)).thenReturn(null)

        val exception = assertThrows<CustomException> {
            service.retainForWithdrawal(1L, LocalDateTime.now())
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }
}
