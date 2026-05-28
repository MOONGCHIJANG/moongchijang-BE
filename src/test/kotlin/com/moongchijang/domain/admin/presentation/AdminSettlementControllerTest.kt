package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminSettlementService
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementDashboardResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementDetailResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementPageResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementStatus
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementStatusFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

class AdminSettlementControllerTest {

    private val adminSettlementService: AdminSettlementService = mock(AdminSettlementService::class.java)
    private val controller = AdminSettlementController(adminSettlementService)

    @Test
    fun `운영자 정산 대시보드 조회는 년월을 서비스로 전달한다`() {
        val response = AdminSettlementDashboardResponse(
            year = 2026,
            month = 5,
            completedSettlementAmount = 100_000,
            scheduledSettlementAmount = 50_000,
            platformFeeAmount = 0,
            totalTransactionAmount = 180_000
        )
        `when`(adminSettlementService.getDashboard(2026, 5)).thenReturn(response)

        val result = controller.getDashboard(2026, 5)

        assertEquals(response, result.body?.data)
        verify(adminSettlementService).getDashboard(2026, 5)
    }

    @Test
    fun `운영자 정산 목록 조회는 년월 상태 페이징을 서비스로 전달한다`() {
        val pageable = PageRequest.of(0, 20)
        val response = AdminSettlementPageResponse(
            content = emptyList(),
            totalElements = 0,
            totalPages = 0,
            number = 0,
            size = 20
        )
        `when`(
            adminSettlementService.getSettlements(
                year = 2026,
                month = 5,
                status = AdminSettlementStatusFilter.COMPLETED,
                pageable = pageable
            )
        ).thenReturn(response)

        val result = controller.getSettlements(2026, 5, AdminSettlementStatusFilter.COMPLETED, pageable)

        assertEquals(response, result.body?.data)
        verify(adminSettlementService).getSettlements(2026, 5, AdminSettlementStatusFilter.COMPLETED, pageable)
    }

    @Test
    fun `운영자 정산 상세 조회는 정산 ID를 서비스로 전달한다`() {
        val response = AdminSettlementDetailResponse(
            settlementId = 10L,
            groupBuyId = 10L,
            storeName = "뭉치장 베이커리",
            productName = "두쫀쿠 1개",
            pickupCompletedDate = LocalDate.of(2026, 5, 20),
            participantCount = 10,
            totalPaymentAmount = 100_000,
            refundDeductionAmount = 20_000,
            platformFeeAmount = 0,
            settlementAmount = 80_000,
            scheduledSettlementDate = LocalDate.of(2026, 5, 23),
            status = AdminSettlementStatus.COMPLETED,
            actionable = false
        )
        `when`(adminSettlementService.getSettlementDetail(10L)).thenReturn(response)

        val result = controller.getSettlementDetail(10L)

        assertEquals(response, result.body?.data)
        verify(adminSettlementService).getSettlementDetail(10L)
    }
}
