package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminDashboardOrderMonitoringService
import com.moongchijang.domain.admin.application.AdminDashboardSummaryService
import com.moongchijang.domain.admin.application.AdminDashboardUrgentRefundService
import com.moongchijang.domain.admin.application.dto.AdminDashboardUnconfirmedOrderResponse
import com.moongchijang.domain.admin.application.dto.AdminDashboardSummaryResponse
import com.moongchijang.domain.admin.application.dto.AdminDashboardUrgentRefundResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.authorization.RequireCurrentRole
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
@RequireCurrentRole(UserRole.ADMIN)
@Tag(name = "Admin", description = "운영자 관리")
class AdminDashboardController(
    private val adminDashboardSummaryService: AdminDashboardSummaryService,
    private val adminDashboardOrderMonitoringService: AdminDashboardOrderMonitoringService,
    private val adminDashboardUrgentRefundService: AdminDashboardUrgentRefundService,
) {

    @GetMapping("/summary")
    @Operation(summary = "운영자 대시보드 운영 관리 요약")
    fun getSummary(): ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(adminDashboardSummaryService.getSummary()))
        return response
    }

    @GetMapping("/dashboard/unconfirmed-orders")
    @Operation(summary = "운영자 대시보드 발주 미확정 모니터링")
    fun getUnconfirmedOrders(
        pageable: Pageable
    ): ResponseEntity<ApiResponse<AdminDashboardUnconfirmedOrderResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(adminDashboardOrderMonitoringService.getUnconfirmedOrders(pageable)))
        return response
    }

    @GetMapping("/dashboard/urgent-refunds")
    @Operation(summary = "운영자 대시보드 긴급 환불 요청 조회")
    fun getUrgentRefunds(
        pageable: Pageable
    ): ResponseEntity<ApiResponse<AdminDashboardUrgentRefundResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(adminDashboardUrgentRefundService.getUrgentRefunds(pageable)))
        return response
    }
}
