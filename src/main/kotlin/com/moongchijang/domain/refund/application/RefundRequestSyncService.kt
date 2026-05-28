package com.moongchijang.domain.refund.application

import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.refund.domain.entity.RefundRequest
import com.moongchijang.domain.refund.domain.repository.RefundRequestRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RefundRequestSyncService(
    private val refundRequestRepository: RefundRequestRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun markApproved(participation: Participation, approvedAmount: Int, at: LocalDateTime) {
        log.info(
            "[RefundRequestSyncService] 환불 요청 승인 동기화 시작: participationId={}, approvedAmount={}",
            participation.id,
            approvedAmount
        )
        val refundRequest = refundRequestRepository.findByParticipationId(participation.id)
            ?: RefundRequest(
                participation = participation,
                requestedAmount = participation.totalAmount,
                requestedAt = participation.cancelledAt ?: participation.createdAt ?: at,
            )
        refundRequest.markApproved(approvedRefundAmount = approvedAmount, at = at)
        refundRequestRepository.save(refundRequest)
        log.info(
            "[RefundRequestSyncService] 환불 요청 승인 동기화 완료: participationId={}, refundRequestId={}, status={}",
            participation.id,
            refundRequest.id,
            refundRequest.status
        )
    }

    fun markRejected(participation: Participation, reason: String?, at: LocalDateTime) {
        log.info(
            "[RefundRequestSyncService] 환불 요청 거절 동기화 시작: participationId={}, reasonPresent={}",
            participation.id,
            !reason.isNullOrBlank()
        )
        val refundRequest = refundRequestRepository.findByParticipationId(participation.id)
            ?: RefundRequest(
                participation = participation,
                requestedAmount = participation.totalAmount,
                requestedAt = participation.cancelledAt ?: participation.createdAt ?: at,
            )
        refundRequest.markRejected(reason = reason, at = at)
        refundRequestRepository.save(refundRequest)
        log.info(
            "[RefundRequestSyncService] 환불 요청 거절 동기화 완료: participationId={}, refundRequestId={}, status={}",
            participation.id,
            refundRequest.id,
            refundRequest.status
        )
    }

    fun markCompleted(participation: Participation, at: LocalDateTime) {
        log.info(
            "[RefundRequestSyncService] 환불 요청 완료 동기화 시작: participationId={}, refundedAt={}",
            participation.id,
            at
        )
        val refundRequest = refundRequestRepository.findByParticipationId(participation.id)
            ?: RefundRequest(
                participation = participation,
                requestedAmount = participation.totalAmount,
                requestedAt = participation.cancelledAt ?: participation.createdAt ?: at,
            )
        refundRequest.markCompleted(at = at)
        refundRequestRepository.save(refundRequest)
        log.info(
            "[RefundRequestSyncService] 환불 요청 완료 동기화 완료: participationId={}, refundRequestId={}, status={}",
            participation.id,
            refundRequest.id,
            refundRequest.status
        )
    }
}
