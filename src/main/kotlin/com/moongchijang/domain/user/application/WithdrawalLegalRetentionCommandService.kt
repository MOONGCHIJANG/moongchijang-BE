package com.moongchijang.domain.user.application

import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.domain.repository.PaymentOrderRepository
import com.moongchijang.domain.refund.domain.repository.RefundRequestRepository
import com.moongchijang.domain.user.domain.entity.WithdrawnParticipation
import com.moongchijang.domain.user.domain.entity.WithdrawnPaymentOrder
import com.moongchijang.domain.user.domain.entity.WithdrawnRefundRequest
import com.moongchijang.domain.user.domain.repository.WithdrawnAccountRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnParticipationRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnPaymentOrderRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnRefundRequestRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WithdrawalLegalRetentionCommandService(
    private val withdrawnAccountRepository: WithdrawnAccountRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val participationRepository: ParticipationRepository,
    private val refundRequestRepository: RefundRequestRepository,
    private val withdrawnPaymentOrderRepository: WithdrawnPaymentOrderRepository,
    private val withdrawnParticipationRepository: WithdrawnParticipationRepository,
    private val withdrawnRefundRequestRepository: WithdrawnRefundRequestRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun retainForWithdrawal(userId: Long, withdrawnAt: LocalDateTime) {
        val withdrawnAccount = withdrawnAccountRepository.findByWithdrawnUserId(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val retentionExpiresAt = withdrawnAt.plusYears(5)

        val paymentOrders = paymentOrderRepository.findAllByUserId(userId)
        if (paymentOrders.isNotEmpty()) {
            withdrawnPaymentOrderRepository.saveAll(
                paymentOrders.map { order ->
                    WithdrawnPaymentOrder(
                        withdrawnUserId = userId,
                        withdrawnAccountId = withdrawnAccount.id,
                        originalPaymentOrderId = order.id,
                        originalGroupBuyId = order.groupBuy.id,
                        orderId = order.orderId,
                        quantity = order.quantity,
                        productAmount = order.productAmount,
                        feeAmount = order.feeAmount,
                        totalAmount = order.totalAmount,
                        agreedNoCancelAfterGoal = order.agreedNoCancelAfterGoal,
                        agreedRefundBeforeGoal = order.agreedRefundBeforeGoal,
                        agreedNoRefundAfterNoShow = order.agreedNoRefundAfterNoShow,
                        agreedNoWithdrawal = order.agreedNoWithdrawal,
                        status = order.status,
                        approvedAt = order.approvedAt,
                        failedAt = order.failedAt,
                        cancelledAt = order.cancelledAt,
                        withdrawnAt = withdrawnAt,
                        retentionExpiresAt = retentionExpiresAt,
                    )
                }
            )
        }

        val participations = participationRepository.findAllByUserId(userId)
        if (participations.isNotEmpty()) {
            val orderIdByGroupBuyId = paymentOrders.associateBy({ it.groupBuy.id }, { it.id })
            withdrawnParticipationRepository.saveAll(
                participations.map { participation ->
                    WithdrawnParticipation(
                        withdrawnUserId = userId,
                        withdrawnAccountId = withdrawnAccount.id,
                        originalParticipationId = participation.id,
                        originalGroupBuyId = participation.groupBuy.id,
                        originalPaymentOrderId = orderIdByGroupBuyId[participation.groupBuy.id],
                        quantity = participation.quantity,
                        productAmount = participation.productAmount,
                        feeAmount = participation.feeAmount,
                        totalAmount = participation.totalAmount,
                        status = participation.status,
                        pickupStatus = participation.pickupStatus,
                        pickupToken = participation.pickupToken,
                        pickedUpAt = participation.pickedUpAt,
                        cancelReason = participation.cancelReason,
                        cancelReasonDetail = participation.cancelReasonDetail,
                        cancelledAt = participation.cancelledAt,
                        refundedAt = participation.refundedAt,
                        approvedRefundAmount = participation.approvedRefundAmount,
                        ownerRefundReviewStatus = participation.ownerRefundReviewStatus,
                        ownerRefundDisputeReason = participation.ownerRefundDisputeReason,
                        ownerRefundReviewedAt = participation.ownerRefundReviewedAt,
                        withdrawnAt = withdrawnAt,
                        retentionExpiresAt = retentionExpiresAt,
                    )
                }
            )
        }

        val refundRequests = refundRequestRepository.findAllByParticipationUserId(userId)
        if (refundRequests.isNotEmpty()) {
            withdrawnRefundRequestRepository.saveAll(
                refundRequests.map { refundRequest ->
                    WithdrawnRefundRequest(
                        withdrawnUserId = userId,
                        withdrawnAccountId = withdrawnAccount.id,
                        originalRefundRequestId = refundRequest.id,
                        originalParticipationId = refundRequest.participation.id,
                        status = refundRequest.status,
                        requestedAmount = refundRequest.requestedAmount,
                        approvedRefundAmount = refundRequest.approvedRefundAmount,
                        rejectedReason = refundRequest.rejectedReason,
                        requestedAt = refundRequest.requestedAt,
                        approvedAt = refundRequest.approvedAt,
                        rejectedAt = refundRequest.rejectedAt,
                        refundedAt = refundRequest.refundedAt,
                        withdrawnAt = withdrawnAt,
                        retentionExpiresAt = retentionExpiresAt,
                    )
                }
            )
        }

        log.info(
            "[WithdrawalLegalRetentionCommandService] 법정보존 데이터 이관 완료: userId={}, paymentOrders={}, participations={}, refundRequests={}",
            userId,
            paymentOrders.size,
            participations.size,
            refundRequests.size,
        )
    }
}
