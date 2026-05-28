package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestCaseFilter
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestListItemResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestPageResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestStatus
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestTab
import com.moongchijang.domain.admin.application.dto.refund.calculateSlaRemainingHours
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
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
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getRefundRequests(
        tab: AdminRefundRequestTab,
        pageable: Pageable,
    ): AdminRefundRequestPageResponse {
        val now = LocalDateTime.now(clock)
        log.info(
            "[AdminRefundRequestService] 환불 요청 목록 조회 시작: tab={}, page={}, size={}",
            tab, pageable.pageNumber, pageable.pageSize
        )
        val page = when (tab) {
            AdminRefundRequestTab.ALL -> participationRepository.findAdminRefundRequestsAll(
                statuses = listOf(ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED),
                pageable = pageable,
            )

            AdminRefundRequestTab.REVIEW_PENDING -> participationRepository.findAdminRefundRequestsByReviewStatusIncludingNull(
                status = ParticipationStatus.REFUND_PENDING,
                reviewStatus = OwnerRefundReviewStatus.PENDING,
                pageable = pageable,
            )

            AdminRefundRequestTab.IN_PROGRESS -> participationRepository.findAdminRefundRequestsByReviewStatus(
                status = ParticipationStatus.REFUND_PENDING,
                reviewStatus = OwnerRefundReviewStatus.APPROVED,
                pageable = pageable,
            )

            AdminRefundRequestTab.APPROVED -> participationRepository.findAdminRefundRequestsAll(
                statuses = listOf(ParticipationStatus.REFUNDED),
                pageable = pageable,
            )

            AdminRefundRequestTab.REJECTED -> participationRepository.findAdminRefundRequestsByReviewStatus(
                status = ParticipationStatus.REFUND_PENDING,
                reviewStatus = OwnerRefundReviewStatus.DISPUTED,
                pageable = pageable,
            )
        }
        log.info(
            "[AdminRefundRequestService] 환불 요청 목록 조회 완료: tab={}, page={}, size={}, totalElements={}",
            tab, pageable.pageNumber, pageable.pageSize, page.totalElements
        )

        return AdminRefundRequestPageResponse(
            content = page.content.map { it.toListItem(now) },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            number = page.number,
            size = page.size,
        )
    }

    private fun Participation.toListItem(now: LocalDateTime): AdminRefundRequestListItemResponse {
        val requestedAt = cancelledAt ?: createdAt ?: now
        return AdminRefundRequestListItemResponse(
            requestId = id,
            caseFilter = cancelReason.toAdminCaseFilter(),
            consumerName = user.nickname ?: "알 수 없음",
            groupBuyName = groupBuy.productName,
            storeName = groupBuy.store.name,
            paymentAmount = totalAmount,
            refundAmount = if (status == ParticipationStatus.REFUNDED) totalAmount else 0,
            ownerOpinion = ownerRefundDisputeReason,
            requestedAt = requestedAt,
            slaRemainingHours = calculateSlaRemainingHours(requestedAt, now),
            status = toAdminStatus(),
        )
    }

    private fun Participation.toAdminStatus(): AdminRefundRequestStatus {
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

    private fun ParticipationCancelReason?.toAdminCaseFilter(): AdminRefundRequestCaseFilter {
        return when (this) {
            ParticipationCancelReason.TIME_UNAVAILABLE -> AdminRefundRequestCaseFilter.PICKUP_PERIOD_NO_SHOW
            ParticipationCancelReason.NO_LONGER_WANTED -> AdminRefundRequestCaseFilter.PRE_ACHIEVEMENT_FREE_CANCEL
            ParticipationCancelReason.PREFER_DIRECT_VISIT -> AdminRefundRequestCaseFilter.POST_ACHIEVEMENT_CANCEL
            ParticipationCancelReason.BOUGHT_ELSEWHERE -> AdminRefundRequestCaseFilter.POST_ACHIEVEMENT_CANCEL
            ParticipationCancelReason.OTHER -> AdminRefundRequestCaseFilter.DISPUTE_OR_DROPOUT_REFUND
            null -> AdminRefundRequestCaseFilter.ALL
        }
    }
}
