package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.settlement.ADMIN_SETTLEMENT_DELAY_DAYS
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementDashboardResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementDetailResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementListItemResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementPageResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementStatus
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementStatusFilter
import com.moongchijang.domain.admin.application.dto.settlement.scheduledSettlementDate
import com.moongchijang.domain.admin.application.dto.settlement.settlementAmount
import com.moongchijang.domain.admin.application.dto.settlement.toSettlementStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

@Service
@Transactional(readOnly = true)
class AdminSettlementService(
    private val participationRepository: ParticipationRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(AdminSettlementService::class.java)

    fun getDashboard(
        year: Int,
        month: Int,
    ): AdminSettlementDashboardResponse {
        log.info("[AdminSettlementService] 정산 대시보드 조회 시작: year={}, month={}", year, month)
        val yearMonth = validateYearMonth(year, month)
        val today = LocalDate.now(clock)
        val range = yearMonth.toDateRange()
        val aggregations = participationRepository.findAdminSettlementAggregations(
            groupBuyStatuses = SETTLEMENT_GROUP_BUY_STATUSES,
            transactionStatuses = TRANSACTION_STATUSES,
            revenueStatuses = REVENUE_STATUSES,
            refundStatuses = REFUND_STATUSES,
            pickupDateFrom = range.from,
            pickupDateTo = range.to
        )

        val response = AdminSettlementDashboardResponse(
            year = year,
            month = month,
            completedSettlementAmount = aggregations
                .filter { it.scheduledSettlementDate().toSettlementStatus(today) == AdminSettlementStatus.COMPLETED }
                .sumOf { it.settlementAmount() },
            scheduledSettlementAmount = aggregations
                .filter { it.scheduledSettlementDate().toSettlementStatus(today) == AdminSettlementStatus.SCHEDULED }
                .sumOf { it.settlementAmount() },
            platformFeeAmount = 0L,
            totalTransactionAmount = aggregations.sumOf { it.totalPaymentAmount }
        )
        log.info("[AdminSettlementService] 정산 대시보드 조회 완료: year={}, month={}", year, month)
        return response
    }

    fun getSettlements(
        year: Int,
        month: Int,
        status: AdminSettlementStatusFilter,
        pageable: Pageable,
    ): AdminSettlementPageResponse {
        log.info(
            "[AdminSettlementService] 정산 목록 조회 시작: year={}, month={}, status={}, page={}, size={}",
            year,
            month,
            status,
            pageable.pageNumber,
            pageable.pageSize,
        )
        val yearMonth = validateYearMonth(year, month)
        val today = LocalDate.now(clock)
        val range = yearMonth.toDateRange().filterByStatus(status, today)
        val page = participationRepository.findAdminSettlementPage(
            groupBuyStatuses = SETTLEMENT_GROUP_BUY_STATUSES,
            transactionStatuses = TRANSACTION_STATUSES,
            revenueStatuses = REVENUE_STATUSES,
            refundStatuses = REFUND_STATUSES,
            pickupDateFrom = range.from,
            pickupDateTo = range.to,
            pageable = pageable
        )

        val response = AdminSettlementPageResponse.from(page, today)
        log.info(
            "[AdminSettlementService] 정산 목록 조회 완료: year={}, month={}, status={}, totalElements={}",
            year,
            month,
            status,
            response.totalElements,
        )
        return response
    }

    fun getSettlementDetail(settlementId: Long): AdminSettlementDetailResponse {
        log.info("[AdminSettlementService] 정산 상세 조회 시작: settlementId={}", settlementId)
        val today = LocalDate.now(clock)
        val aggregation = participationRepository.findAdminSettlementDetail(
            groupBuyId = settlementId,
            groupBuyStatuses = SETTLEMENT_GROUP_BUY_STATUSES,
            transactionStatuses = TRANSACTION_STATUSES,
            revenueStatuses = REVENUE_STATUSES,
            refundStatuses = REFUND_STATUSES,
        ) ?: throw CustomException(ErrorCode.GROUPBUY_NOT_FOUND)

        val response = AdminSettlementListItemResponse.from(aggregation, today)
        log.info("[AdminSettlementService] 정산 상세 조회 완료: settlementId={}", settlementId)
        return response
    }

    private fun validateYearMonth(year: Int, month: Int): YearMonth {
        if (year !in 2000..2100 || month !in 1..12) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }

        return YearMonth.of(year, month)
    }

    private fun YearMonth.toDateRange(): DateRange =
        DateRange(atDay(1), plusMonths(1).atDay(1))

    private fun DateRange.filterByStatus(
        status: AdminSettlementStatusFilter,
        today: LocalDate,
    ): DateRange {
        val completedToExclusive = today.minusDays(ADMIN_SETTLEMENT_DELAY_DAYS).plusDays(1)
        return when (status) {
            AdminSettlementStatusFilter.SCHEDULED -> DateRange(
                from = maxOf(from, completedToExclusive),
                to = to
            )
            AdminSettlementStatusFilter.COMPLETED -> DateRange(
                from = from,
                to = minOf(to, completedToExclusive)
            )
            AdminSettlementStatusFilter.ALL -> this
        }
    }

    private data class DateRange(
        val from: LocalDate,
        val to: LocalDate,
    )

    private companion object {
        val SETTLEMENT_GROUP_BUY_STATUSES = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
        val REVENUE_STATUSES = listOf(ParticipationStatus.CONFIRMED)
        val REFUND_STATUSES = listOf(ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED)
        val TRANSACTION_STATUSES = REVENUE_STATUSES + REFUND_STATUSES
    }
}
