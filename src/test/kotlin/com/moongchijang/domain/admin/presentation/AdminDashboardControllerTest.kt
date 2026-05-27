package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminDashboardOrderMonitoringService
import com.moongchijang.domain.admin.application.AdminDashboardSummaryService
import com.moongchijang.domain.admin.application.dto.AdminDashboardSummaryResponse
import com.moongchijang.domain.admin.application.dto.AdminDashboardUnconfirmedOrderResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class AdminDashboardControllerTest {

    private val adminDashboardSummaryService: AdminDashboardSummaryService = mock(AdminDashboardSummaryService::class.java)
    private val adminDashboardOrderMonitoringService: AdminDashboardOrderMonitoringService =
        mock(AdminDashboardOrderMonitoringService::class.java)
    private val controller = AdminDashboardController(
        adminDashboardSummaryService = adminDashboardSummaryService,
        adminDashboardOrderMonitoringService = adminDashboardOrderMonitoringService
    )

    @Test
    fun `운영자 대시보드 요약 조회는 서비스 결과를 반환한다`() {
        val response = AdminDashboardSummaryResponse(
            pendingRefundAmount = 10_000L,
            pendingRefundAmountChangeRate = 10.0,
            pendingApprovalCount = 3L,
            averageReviewMinutes = 45L,
            pendingApprovalChangeRate = -20.0,
            unconfirmedOrderCount = 0L,
            unconfirmedOrderOver48hCount = 0L,
            todayCompletedRefundCount = 2L,
            todayCompletedApprovalCount = 4L,
            hasOrderOver48h = false
        )
        `when`(adminDashboardSummaryService.getSummary()).thenReturn(response)

        val result = controller.getSummary()

        assertEquals(response, result.body?.data)
        verify(adminDashboardSummaryService).getSummary()
    }

    @Test
    fun `운영자 대시보드 발주 미확정 모니터링은 페이징을 서비스로 전달한다`() {
        val pageable = PageRequest.of(0, 5)
        val response = AdminDashboardUnconfirmedOrderResponse(
            totalUnconfirmedCount = 0L,
            overdueCount = 0L,
            hasOverdue = false,
            content = emptyList(),
            totalElements = 0L,
            totalPages = 0,
            number = 0,
            size = 5
        )
        `when`(adminDashboardOrderMonitoringService.getUnconfirmedOrders(pageable)).thenReturn(response)

        val result = controller.getUnconfirmedOrders(pageable)

        assertEquals(response, result.body?.data)
        verify(adminDashboardOrderMonitoringService).getUnconfirmedOrders(pageable)
    }
}
