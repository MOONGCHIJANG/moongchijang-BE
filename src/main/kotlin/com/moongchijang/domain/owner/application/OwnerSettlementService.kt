package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundRequestDetailResponse
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundRequestListItemResponse
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundRequestListResponse
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundRequestStatus
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundRequestTab
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundReviewActionType
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundReviewSubmitRequest
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundReviewSubmitResponse
import com.moongchijang.domain.owner.application.dto.settlement.OwnerSettlementMonthChipListResponse
import com.moongchijang.domain.owner.application.dto.settlement.OwnerSettlementMonthChipResponse
import com.moongchijang.domain.owner.application.dto.settlement.OwnerSettlementMonthlySummaryResponse
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.refund.application.RefundRequestSyncService
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class OwnerSettlementService(
    private val userRepository: UserRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val refundRequestSyncService: RefundRequestSyncService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getMonthlySettlementSummary(ownerId: Long, year: Int, month: Int): OwnerSettlementMonthlySummaryResponse {
        log.info("[OwnerSettlementService] 월별 정산 요약 조회 시작: ownerId={}, year={}, month={}", ownerId, year, month)
        validateSeller(ownerId)
        validateYearMonth(year, month)

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            return OwnerSettlementMonthlySummaryResponse(
                year = year,
                month = month,
                settlementExpectedAmount = 0L,
                grossRevenueAmount = 0L,
                refundFeeAmount = 0L,
            )
        }

        val grossRevenueAmount = participationRepository.sumTotalAmountByStoreIdsAndStatusesAndYearMonth(
            storeIds = storeIds,
            groupBuyStatuses = SETTLEMENT_GROUP_BUY_STATUSES,
            participationStatuses = SETTLEMENT_PARTICIPATION_STATUSES,
            year = year,
            month = month,
        )
        val refundFeeAmount = participationRepository.sumRefundFeeAmountByStoreIdsAndYearMonth(
            storeIds = storeIds,
            refundStatuses = REFUND_STATUSES,
            year = year,
            month = month,
        )
        val settlementExpectedAmount = (grossRevenueAmount + refundFeeAmount).coerceAtLeast(0L)

        return OwnerSettlementMonthlySummaryResponse(
            year = year,
            month = month,
            settlementExpectedAmount = settlementExpectedAmount,
            grossRevenueAmount = grossRevenueAmount,
            refundFeeAmount = refundFeeAmount,
        )
    }

    fun getSettlementMonthChips(ownerId: Long): OwnerSettlementMonthChipListResponse {
        log.info("[OwnerSettlementService] 정산 월 칩 조회 시작: ownerId={}", ownerId)
        validateSeller(ownerId)

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            return OwnerSettlementMonthChipListResponse(emptyList())
        }

        val chips = groupBuyRepository.findDistinctPickupDatesByStoreIdsAndStatuses(
            storeIds = storeIds,
            statuses = SETTLEMENT_GROUP_BUY_STATUSES,
        )
            .map { YearMonth.from(it) }
            .distinct()
            .sortedDescending()
            .map {
                OwnerSettlementMonthChipResponse(
                    year = it.year,
                    month = it.monthValue,
                    label = "${it.year}년 ${it.monthValue}월",
                )
            }

        return OwnerSettlementMonthChipListResponse(chips)
    }

    fun getRefundRequests(ownerId: Long, tab: OwnerRefundRequestTab): OwnerRefundRequestListResponse {
        log.info("[OwnerSettlementService] 환불 요청 목록 조회 시작: ownerId={}, tab={}", ownerId, tab)
        validateSeller(ownerId)

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            return OwnerRefundRequestListResponse(
                pendingCount = 0,
                completedCount = 0,
                hasPendingItems = false,
                items = emptyList(),
            )
        }

        val allRequests = participationRepository.findRefundRequestsByStoreIdsAndStatuses(
            storeIds = storeIds,
            statuses = REFUND_STATUSES,
            fromDateTime = LocalDate.now(SEOUL_ZONE_ID).minusMonths(REFUND_LIST_LOOKBACK_MONTHS).atStartOfDay(),
        )

        val pendingCount = allRequests.count { request -> isReviewPending(request) }
        val completedCount = allRequests.count { request -> isReviewCompleted(request) }

        val filtered = when (tab) {
            OwnerRefundRequestTab.ALL -> allRequests
            OwnerRefundRequestTab.PENDING -> allRequests.filter { request -> isReviewPending(request) }
            OwnerRefundRequestTab.COMPLETED -> allRequests.filter { request -> isReviewCompleted(request) }
        }

        return OwnerRefundRequestListResponse(
            pendingCount = pendingCount,
            completedCount = completedCount,
            hasPendingItems = pendingCount > 0,
            items = filtered.map { toRefundListItem(it) },
        )
    }

    fun getRefundRequestDetail(ownerId: Long, participationId: Long): OwnerRefundRequestDetailResponse {
        log.info("[OwnerSettlementService] 환불 요청 상세 조회 시작: ownerId={}, participationId={}", ownerId, participationId)
        validateSeller(ownerId)

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val participation = participationRepository.findPickupDetailById(participationId)
            ?: throw CustomException(ErrorCode.PARTICIPATION_NOT_FOUND)
        if (participation.groupBuy.store.id !in storeIds || participation.status !in REFUND_STATUSES) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val penaltyAmount = participation.feeAmount.coerceAtLeast(0)
        val refundExpectedAmount = (participation.totalAmount - penaltyAmount).coerceAtLeast(0)
        return OwnerRefundRequestDetailResponse(
            participationId = participation.id,
            groupBuyId = participation.groupBuy.id,
            productName = participation.groupBuy.productName,
            requesterName = participation.user.nickname ?: "",
            requestedDate = (participation.cancelledAt ?: participation.createdAt ?: java.time.LocalDateTime.now()).toLocalDate(),
            paymentAmount = participation.totalAmount,
            penaltyAmount = penaltyAmount,
            refundExpectedAmount = refundExpectedAmount,
            refundReasonDetail = participation.cancelReasonDetail,
            status = toRefundStatus(participation.status),
        )
    }

    @Transactional
    fun submitRefundReview(
        ownerId: Long,
        participationId: Long,
        request: OwnerRefundReviewSubmitRequest,
    ): OwnerRefundReviewSubmitResponse {
        log.info(
            "[OwnerSettlementService] 환불 요청 검토 제출 시작: ownerId={}, participationId={}, action={}",
            ownerId,
            participationId,
            request.action,
        )
        validateSeller(ownerId)
        validateRefundReviewRequest(request)

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val participation = participationRepository.findByIdForUpdate(participationId).orElseThrow {
            CustomException(ErrorCode.PARTICIPATION_NOT_FOUND)
        }
        if (participation.groupBuy.store.id !in storeIds) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
        if (participation.status != ParticipationStatus.REFUND_PENDING) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        if (isReviewCompleted(participation)) {
            throw CustomException(ErrorCode.OWNER_REFUND_REVIEW_ALREADY_PROCESSED)
        }

        val now = java.time.LocalDateTime.now()
        participation.ownerRefundReviewedAt = now

        when (request.action) {
            OwnerRefundReviewActionType.APPROVE -> {
                val approvedAmount = if (participation.cancelReason == null) {
                    participation.totalAmount
                } else {
                    (participation.totalAmount - participation.feeAmount.coerceAtLeast(0)).coerceAtLeast(0)
                }
                participation.ownerRefundReviewStatus = OwnerRefundReviewStatus.APPROVED
                participation.approvedRefundAmount = approvedAmount
                participation.ownerRefundDisputeReason = null
                refundRequestSyncService.markApproved(
                    participation = participation,
                    approvedAmount = approvedAmount,
                    at = now,
                )
            }
            OwnerRefundReviewActionType.DISPUTE -> {
                val disputeReason = request.disputeReason?.trim()?.takeIf { it.isNotBlank() }
                participation.ownerRefundReviewStatus = OwnerRefundReviewStatus.DISPUTED
                participation.approvedRefundAmount = null
                participation.ownerRefundDisputeReason = disputeReason
                refundRequestSyncService.markRejected(
                    participation = participation,
                    reason = disputeReason,
                    at = now,
                )
            }
        }

        log.info(
            "[OwnerSettlementService] 환불 요청 검토 제출 완료: ownerId={}, participationId={}, action={}",
            ownerId,
            participationId,
            request.action,
        )
        return OwnerRefundReviewSubmitResponse(
            participationId = participation.id,
            processed = true,
        )
    }

    private fun toRefundListItem(participation: Participation): OwnerRefundRequestListItemResponse {
        val requestedAt = participation.cancelledAt ?: participation.createdAt ?: java.time.LocalDateTime.now()
        return OwnerRefundRequestListItemResponse(
            participationId = participation.id,
            groupBuyId = participation.groupBuy.id,
            productName = participation.groupBuy.productName,
            paymentAmount = participation.totalAmount,
            requesterName = participation.user.nickname ?: "",
            requesterCode = "P${participation.id.toString().padStart(3, '0')}",
            refundReasonLabel = toRefundReasonLabel(participation.cancelReason),
            requestedDate = requestedAt.toLocalDate(),
            status = toRefundStatus(participation.status),
            exceeded24Hours = participation.status == ParticipationStatus.REFUND_PENDING &&
                requestedAt.isBefore(java.time.LocalDateTime.now().minusHours(24)),
        )
    }

    private fun toRefundStatus(status: ParticipationStatus): OwnerRefundRequestStatus {
        return if (status == ParticipationStatus.REFUNDED) {
            OwnerRefundRequestStatus.COMPLETED
        } else {
            OwnerRefundRequestStatus.PENDING
        }
    }

    private fun toRefundReasonLabel(reason: ParticipationCancelReason?): String {
        return when (reason) {
            ParticipationCancelReason.TIME_UNAVAILABLE -> "픽업 불가"
            ParticipationCancelReason.NO_LONGER_WANTED -> "구매 의사 없음"
            ParticipationCancelReason.PREFER_DIRECT_VISIT -> "매장 직접 구매"
            ParticipationCancelReason.BOUGHT_ELSEWHERE -> "타 채널 구매"
            ParticipationCancelReason.OTHER -> "기타"
            null -> "기타"
        }
    }

    private fun validateRefundReviewRequest(request: OwnerRefundReviewSubmitRequest) {
        if (request.action == OwnerRefundReviewActionType.DISPUTE && request.disputeReason.isNullOrBlank()) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun isReviewPending(participation: Participation): Boolean {
        return participation.status == ParticipationStatus.REFUND_PENDING &&
            participation.ownerRefundReviewStatus != OwnerRefundReviewStatus.APPROVED &&
            participation.ownerRefundReviewStatus != OwnerRefundReviewStatus.DISPUTED
    }

    private fun isReviewCompleted(participation: Participation): Boolean {
        return participation.ownerRefundReviewStatus == OwnerRefundReviewStatus.APPROVED ||
            participation.ownerRefundReviewStatus == OwnerRefundReviewStatus.DISPUTED ||
            participation.status == ParticipationStatus.REFUNDED
    }

    private fun validateYearMonth(year: Int, month: Int) {
        if (year !in 2000..2100 || month !in 1..12) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        val requested = YearMonth.of(year, month)
        if (requested.isAfter(YearMonth.from(LocalDate.now(SEOUL_ZONE_ID)))) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun validateSeller(ownerId: Long) {
        val owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (owner.role != UserRole.SELLER) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
    }

    private companion object {
        val SETTLEMENT_GROUP_BUY_STATUSES = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
        val SETTLEMENT_PARTICIPATION_STATUSES = listOf(ParticipationStatus.CONFIRMED)
        val REFUND_STATUSES = listOf(ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED)
        val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        const val REFUND_LIST_LOOKBACK_MONTHS: Long = 6
    }
}
