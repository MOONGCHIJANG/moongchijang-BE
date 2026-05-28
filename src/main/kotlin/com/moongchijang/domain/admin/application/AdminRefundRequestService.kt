package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestCaseFilter
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestListItemResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestPageResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestTab
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
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

        return AdminRefundRequestPageResponse(
            content = page.content.map { AdminRefundRequestListItemResponse.from(it, now) },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            number = page.number,
            size = page.size,
        )
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
}
