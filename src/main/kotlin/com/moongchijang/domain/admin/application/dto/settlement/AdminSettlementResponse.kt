package com.moongchijang.domain.admin.application.dto.settlement

import com.moongchijang.domain.participation.domain.repository.AdminSettlementAggregation
import org.springframework.data.domain.Page
import java.time.LocalDate

enum class AdminSettlementStatusFilter {
    SCHEDULED,
    COMPLETED,
    ALL
}

enum class AdminSettlementStatus {
    SCHEDULED,
    COMPLETED
}

data class AdminSettlementDashboardResponse(
    val year: Int,
    val month: Int,
    val completedSettlementAmount: Long,
    val scheduledSettlementAmount: Long,
    val platformFeeAmount: Long,
    val totalTransactionAmount: Long,
)

data class AdminSettlementPageResponse(
    val content: List<AdminSettlementListItemResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
) {
    companion object {
        fun from(
            page: Page<AdminSettlementAggregation>,
            today: LocalDate,
        ): AdminSettlementPageResponse =
            AdminSettlementPageResponse(
                content = page.content.map { AdminSettlementListItemResponse.from(it, today) },
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size
            )
    }
}

data class AdminSettlementListItemResponse(
    val settlementId: Long,
    val groupBuyId: Long,
    val storeName: String,
    val productName: String,
    val pickupCompletedDate: LocalDate,
    val participantCount: Long,
    val totalPaymentAmount: Long,
    val refundDeductionAmount: Long,
    val platformFeeAmount: Long,
    val settlementAmount: Long,
    val scheduledSettlementDate: LocalDate,
    val status: AdminSettlementStatus,
    val actionable: Boolean,
) {
    companion object {
        fun from(
            aggregation: AdminSettlementAggregation,
            today: LocalDate,
        ): AdminSettlementListItemResponse {
            val scheduledSettlementDate = aggregation.scheduledSettlementDate()
            val status = scheduledSettlementDate.toSettlementStatus(today)
            return AdminSettlementListItemResponse(
                settlementId = aggregation.groupBuyId,
                groupBuyId = aggregation.groupBuyId,
                storeName = aggregation.storeName,
                productName = aggregation.productName,
                pickupCompletedDate = aggregation.pickupCompletedDate,
                participantCount = aggregation.participantCount,
                totalPaymentAmount = aggregation.totalPaymentAmount,
                refundDeductionAmount = aggregation.refundDeductionAmount,
                platformFeeAmount = 0L,
                settlementAmount = aggregation.settlementAmount(),
                scheduledSettlementDate = scheduledSettlementDate,
                status = status,
                actionable = status == AdminSettlementStatus.SCHEDULED
            )
        }
    }
}

typealias AdminSettlementDetailResponse = AdminSettlementListItemResponse

fun AdminSettlementAggregation.settlementAmount(): Long =
    (totalPaymentAmount - refundDeductionAmount).coerceAtLeast(0L)

fun AdminSettlementAggregation.scheduledSettlementDate(): LocalDate =
    pickupCompletedDate.plusDays(ADMIN_SETTLEMENT_DELAY_DAYS)

fun LocalDate.toSettlementStatus(today: LocalDate): AdminSettlementStatus =
    if (isAfter(today)) AdminSettlementStatus.SCHEDULED else AdminSettlementStatus.COMPLETED

const val ADMIN_SETTLEMENT_DELAY_DAYS: Long = 3
