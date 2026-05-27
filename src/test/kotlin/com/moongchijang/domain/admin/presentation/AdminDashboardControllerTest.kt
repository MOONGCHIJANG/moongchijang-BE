package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminDashboardSummaryService
import com.moongchijang.domain.admin.application.dto.AdminDashboardSummaryResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class AdminDashboardControllerTest {

    private val adminDashboardSummaryService: AdminDashboardSummaryService = mock(AdminDashboardSummaryService::class.java)
    private val controller = AdminDashboardController(adminDashboardSummaryService)

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
}
