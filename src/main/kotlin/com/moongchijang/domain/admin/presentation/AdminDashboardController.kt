package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminDashboardOrderMonitoringService
import com.moongchijang.domain.admin.application.AdminDashboardSummaryService
import com.moongchijang.domain.admin.application.dto.AdminDashboardUnconfirmedOrderResponse
import com.moongchijang.domain.admin.application.dto.AdminDashboardSummaryResponse
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "운영자 관리")
class AdminDashboardController(
    private val adminDashboardSummaryService: AdminDashboardSummaryService,
    private val adminDashboardOrderMonitoringService: AdminDashboardOrderMonitoringService,
) {

    @GetMapping("/summary")
    @Operation(summary = "운영자 대시보드 운영 관리 요약")
    fun getSummary(): ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> =
        ResponseEntity.ok(ApiResponse.success(adminDashboardSummaryService.getSummary()))

    @GetMapping("/dashboard/unconfirmed-orders")
    @Operation(summary = "운영자 대시보드 발주 미확정 모니터링")
    fun getUnconfirmedOrders(
        pageable: Pageable
    ): ResponseEntity<ApiResponse<AdminDashboardUnconfirmedOrderResponse>> =
        ResponseEntity.ok(ApiResponse.success(adminDashboardOrderMonitoringService.getUnconfirmedOrders(pageable)))
}
