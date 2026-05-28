package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestCaseFilter
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestApproveRequest
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestDetailResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestListItemResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestPageResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestStatus
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestTab
import com.moongchijang.domain.admin.application.refund.AdminRefundRequestStatusTransitionPolicy
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.domain.repository.PaymentOrderRepository
import com.moongchijang.domain.payment.domain.repository.PaymentRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AdminRefundRequestService(
    private val participationRepository: ParticipationRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val paymentRepository: PaymentRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getRefundRequests(
        tab: AdminRefundRequestTab,
        caseFilter: AdminRefundRequestCaseFilter,
        keyword: String?,
        pageable: Pageable,
    ): AdminRefundRequestPageResponse {
        val now = LocalDateTime.now(clock)
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }
        val statuses = tab.toParticipationStatuses()
        val reviewStatuses = tab.toOwnerReviewStatuses()
        val includeNullReviewStatus = tab == AdminRefundRequestTab.REVIEW_PENDING
        val cancelReasons = caseFilter.toCancelReasons()
        log.info(
            "[AdminRefundRequestService] 환불 요청 목록 조회 시작: tab={}, caseFilter={}, keyword={}, page={}, size={}",
            tab, caseFilter, normalizedKeyword, pageable.pageNumber, pageable.pageSize
        )
        val page = participationRepository.findAdminRefundRequests(
            statuses = statuses,
            useReviewStatusFilter = reviewStatuses.isNotEmpty() || includeNullReviewStatus,
            reviewStatuses = reviewStatuses.ifEmpty { listOf(OwnerRefundReviewStatus.PENDING) },
            includeNullReviewStatus = includeNullReviewStatus,
            useCaseFilter = cancelReasons.isNotEmpty(),
            cancelReasons = cancelReasons.ifEmpty { listOf(ParticipationCancelReason.OTHER) },
            keyword = normalizedKeyword,
            pageable = pageable,
        )
        log.info(
            "[AdminRefundRequestService] 환불 요청 목록 조회 완료: tab={}, caseFilter={}, keyword={}, page={}, size={}, totalElements={}",
            tab, caseFilter, normalizedKeyword, pageable.pageNumber, pageable.pageSize, page.totalElements
        )

        val items = page.content.map { AdminRefundRequestListItemResponse.from(it, now) }
        val slaWarningCount = items.count { it.slaWarning }

        return AdminRefundRequestPageResponse(
            content = items,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            number = page.number,
            size = page.size,
            hasSlaWarning = slaWarningCount > 0,
            slaWarningCount = slaWarningCount,
        )
    }

    fun getRefundRequestDetail(requestId: Long): AdminRefundRequestDetailResponse {
        log.info("[AdminRefundRequestService] 환불 요청 상세 조회 시작: requestId={}", requestId)
        val now = LocalDateTime.now(clock)
        val participation = participationRepository.findPickupDetailById(requestId)
            ?: throw CustomException(ErrorCode.PARTICIPATION_NOT_FOUND)
        val paymentOrder = paymentOrderRepository.findByUserIdAndGroupBuyId(
            userId = participation.user.id ?: throw CustomException(ErrorCode.USER_NOT_FOUND),
            groupBuyId = participation.groupBuy.id,
        )
        val payment = paymentOrder?.let { paymentRepository.findByPaymentOrderOrderId(it.orderId) }
        val response = AdminRefundRequestDetailResponse.from(
            participation = participation,
            paymentOrder = paymentOrder,
            payment = payment,
            now = now,
        )
        log.info("[AdminRefundRequestService] 환불 요청 상세 조회 완료: requestId={}, status={}", requestId, response.status)
        return response
    }

    @Transactional
    fun approveRefundRequest(
        requestId: Long,
        request: AdminRefundRequestApproveRequest,
    ): AdminRefundRequestDetailResponse {
        log.info(
            "[AdminRefundRequestService] 환불 요청 승인 시작: requestId={}, refundAmount={}",
            requestId,
            request.refundAmount
        )
        val now = LocalDateTime.now(clock)
        val participation = participationRepository.findByIdForUpdate(requestId)
            .orElseThrow { CustomException(ErrorCode.PARTICIPATION_NOT_FOUND) }

        val currentStatus = participation.toAdminStatus()
        AdminRefundRequestStatusTransitionPolicy.validateTransition(
            from = currentStatus,
            to = AdminRefundRequestStatus.IN_PROGRESS,
        )

        if (request.refundAmount > participation.totalAmount) {
            throw CustomException(ErrorCode.INVALID_INPUT, "refundAmount는 결제 금액을 초과할 수 없습니다.")
        }

        participation.ownerRefundReviewStatus = OwnerRefundReviewStatus.APPROVED
        participation.ownerRefundReviewedAt = now
        participation.ownerRefundDisputeReason = null

        val paymentOrder = paymentOrderRepository.findByUserIdAndGroupBuyId(
            userId = participation.user.id ?: throw CustomException(ErrorCode.USER_NOT_FOUND),
            groupBuyId = participation.groupBuy.id,
        )
        val payment = paymentOrder?.let { paymentRepository.findByPaymentOrderOrderId(it.orderId) }
        val response = AdminRefundRequestDetailResponse.from(
            participation = participation,
            paymentOrder = paymentOrder,
            payment = payment,
            now = now,
        )
        log.info(
            "[AdminRefundRequestService] 환불 요청 승인 완료: requestId={}, status={}",
            requestId,
            response.status
        )
        return response
    }

    private fun AdminRefundRequestTab.toParticipationStatuses(): List<ParticipationStatus> {
        return when (this) {
            AdminRefundRequestTab.ALL -> listOf(ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED)
            AdminRefundRequestTab.REVIEW_PENDING,
            AdminRefundRequestTab.IN_PROGRESS,
            AdminRefundRequestTab.REJECTED -> listOf(ParticipationStatus.REFUND_PENDING)
            AdminRefundRequestTab.APPROVED -> listOf(ParticipationStatus.REFUNDED)
        }
    }

    private fun AdminRefundRequestTab.toOwnerReviewStatuses(): List<OwnerRefundReviewStatus> {
        return when (this) {
            AdminRefundRequestTab.ALL,
            AdminRefundRequestTab.APPROVED -> emptyList()
            AdminRefundRequestTab.REVIEW_PENDING -> listOf(OwnerRefundReviewStatus.PENDING)
            AdminRefundRequestTab.IN_PROGRESS -> listOf(OwnerRefundReviewStatus.APPROVED)
            AdminRefundRequestTab.REJECTED -> listOf(OwnerRefundReviewStatus.DISPUTED)
        }
    }

    private fun AdminRefundRequestCaseFilter.toCancelReasons(): List<ParticipationCancelReason> {
        return when (this) {
            AdminRefundRequestCaseFilter.ALL -> emptyList()
            AdminRefundRequestCaseFilter.PRE_ACHIEVEMENT_FREE_CANCEL -> listOf(ParticipationCancelReason.NO_LONGER_WANTED)
            AdminRefundRequestCaseFilter.POST_ACHIEVEMENT_CANCEL -> listOf(
                ParticipationCancelReason.PREFER_DIRECT_VISIT,
                ParticipationCancelReason.BOUGHT_ELSEWHERE,
            )
            AdminRefundRequestCaseFilter.PICKUP_PERIOD_NO_SHOW -> listOf(ParticipationCancelReason.TIME_UNAVAILABLE)
            AdminRefundRequestCaseFilter.OWNER_FAULT_CANCEL -> emptyList()
            AdminRefundRequestCaseFilter.TARGET_NOT_MET -> emptyList()
            AdminRefundRequestCaseFilter.DISPUTE_OR_DROPOUT_REFUND -> listOf(ParticipationCancelReason.OTHER)
        }
    }

    private fun com.moongchijang.domain.participation.domain.entity.Participation.toAdminStatus(): AdminRefundRequestStatus {
        if (status == ParticipationStatus.REFUNDED) {
            return AdminRefundRequestStatus.APPROVED
        }
        return when (ownerRefundReviewStatus) {
            OwnerRefundReviewStatus.APPROVED -> AdminRefundRequestStatus.IN_PROGRESS
            OwnerRefundReviewStatus.DISPUTED -> AdminRefundRequestStatus.REJECTED
            OwnerRefundReviewStatus.PENDING,
            null -> AdminRefundRequestStatus.REVIEW_PENDING
        }
    }
}
