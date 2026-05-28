package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementStatus
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementStatusFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.AdminSettlementAggregation
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.exception.CustomException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AdminSettlementServiceTest {

    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-28T04:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminSettlementService(participationRepository, clock)

    @Test
    fun `월별 정산 대시보드를 조회한다`() {
        val completed = aggregation(
            groupBuyId = 10L,
            pickupCompletedDate = LocalDate.of(2026, 5, 20),
            totalPaymentAmount = 120_000L,
            refundDeductionAmount = 20_000L,
            platformFeeAmount = 0L
        )
        val scheduled = aggregation(
            groupBuyId = 11L,
            pickupCompletedDate = LocalDate.of(2026, 5, 27),
            totalPaymentAmount = 80_000L,
            refundDeductionAmount = 0L,
            platformFeeAmount = 0L
        )
        `when`(
            participationRepository.findAdminSettlementAggregations(
                SETTLEMENT_GROUP_BUY_STATUSES,
                TRANSACTION_STATUSES,
                REVENUE_STATUSES,
                REFUND_STATUSES,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1)
            )
        ).thenReturn(listOf(completed, scheduled))

        val result = service.getDashboard(2026, 5)

        assertEquals(2026, result.year)
        assertEquals(5, result.month)
        assertEquals(100_000L, result.completedSettlementAmount)
        assertEquals(80_000L, result.scheduledSettlementAmount)
        assertEquals(0L, result.platformFeeAmount)
        assertEquals(200_000L, result.totalTransactionAmount)
    }

    @Test
    fun `정산 예정 목록은 예정 상태 pickupDate 범위로 조회한다`() {
        val pageable = PageRequest.of(0, 20)
        val scheduled = aggregation(
            groupBuyId = 12L,
            pickupCompletedDate = LocalDate.of(2026, 5, 27),
            totalPaymentAmount = 50_000L,
            refundDeductionAmount = 10_000L,
            platformFeeAmount = 0L
        )
        `when`(
            participationRepository.findAdminSettlementPage(
                SETTLEMENT_GROUP_BUY_STATUSES,
                TRANSACTION_STATUSES,
                REVENUE_STATUSES,
                REFUND_STATUSES,
                LocalDate.of(2026, 5, 26),
                LocalDate.of(2026, 6, 1),
                pageable
            )
        ).thenReturn(PageImpl(listOf(scheduled), pageable, 1))

        val result = service.getSettlements(2026, 5, AdminSettlementStatusFilter.SCHEDULED, pageable)

        assertEquals(1, result.content.size)
        assertEquals(12L, result.content[0].settlementId)
        assertEquals(LocalDate.of(2026, 5, 30), result.content[0].scheduledSettlementDate)
        assertEquals(AdminSettlementStatus.SCHEDULED, result.content[0].status)
        assertEquals(40_000L, result.content[0].settlementAmount)
        assertTrue(result.content[0].actionable)
    }

    @Test
    fun `정산 상세를 조회한다`() {
        val aggregation = aggregation(
            groupBuyId = 13L,
            pickupCompletedDate = LocalDate.of(2026, 5, 23),
            totalPaymentAmount = 100_000L,
            refundDeductionAmount = 20_000L,
            platformFeeAmount = 0L
        )
        `when`(
            participationRepository.findAdminSettlementDetail(
                13L,
                SETTLEMENT_GROUP_BUY_STATUSES,
                TRANSACTION_STATUSES,
                REVENUE_STATUSES,
                REFUND_STATUSES
            )
        ).thenReturn(aggregation)

        val result = service.getSettlementDetail(13L)

        assertEquals(13L, result.settlementId)
        assertEquals("뭉치장 베이커리", result.storeName)
        assertEquals(AdminSettlementStatus.COMPLETED, result.status)
        assertEquals(80_000L, result.settlementAmount)
        assertFalse(result.status == AdminSettlementStatus.SCHEDULED)
    }

    @Test
    fun `존재하지 않는 정산 상세는 예외를 던진다`() {
        `when`(
            participationRepository.findAdminSettlementDetail(
                404L,
                SETTLEMENT_GROUP_BUY_STATUSES,
                TRANSACTION_STATUSES,
                REVENUE_STATUSES,
                REFUND_STATUSES
            )
        ).thenReturn(null)

        assertThrows(CustomException::class.java) {
            service.getSettlementDetail(404L)
        }
    }

    @Test
    fun `잘못된 월은 예외를 던진다`() {
        assertThrows(CustomException::class.java) {
            service.getDashboard(2026, 13)
        }
    }

    @Test
    fun `전체 목록은 월 전체 pickupDate 범위로 조회한다`() {
        val pageable = PageRequest.of(0, 20)
        `when`(
            participationRepository.findAdminSettlementPage(
                SETTLEMENT_GROUP_BUY_STATUSES,
                TRANSACTION_STATUSES,
                REVENUE_STATUSES,
                REFUND_STATUSES,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                pageable
            )
        ).thenReturn(PageImpl(emptyList(), pageable, 0))

        service.getSettlements(2026, 5, AdminSettlementStatusFilter.ALL, pageable)

        verify(participationRepository).findAdminSettlementPage(
            SETTLEMENT_GROUP_BUY_STATUSES,
            TRANSACTION_STATUSES,
            REVENUE_STATUSES,
            REFUND_STATUSES,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 6, 1),
            pageable
        )
    }

    private fun aggregation(
        groupBuyId: Long,
        pickupCompletedDate: LocalDate,
        totalPaymentAmount: Long,
        refundDeductionAmount: Long,
        platformFeeAmount: Long,
    ): AdminSettlementAggregation =
        object : AdminSettlementAggregation {
            override val groupBuyId: Long = groupBuyId
            override val storeName: String = "뭉치장 베이커리"
            override val productName: String = "두쫀쿠 1개"
            override val pickupCompletedDate: LocalDate = pickupCompletedDate
            override val participantCount: Long = 10L
            override val totalPaymentAmount: Long = totalPaymentAmount
            override val refundDeductionAmount: Long = refundDeductionAmount
            override val platformFeeAmount: Long = platformFeeAmount
        }

    private companion object {
        val SETTLEMENT_GROUP_BUY_STATUSES = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
        val REVENUE_STATUSES = listOf(ParticipationStatus.CONFIRMED)
        val REFUND_STATUSES = listOf(ParticipationStatus.REFUND_PENDING, ParticipationStatus.REFUNDED)
        val TRANSACTION_STATUSES = REVENUE_STATUSES + REFUND_STATUSES
    }
}
