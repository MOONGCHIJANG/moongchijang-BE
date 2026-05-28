package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminSettlementService
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementDashboardResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementDetailResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementPageResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementStatusFilter
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/settlements")
@Tag(name = "Admin", description = "운영자 관리")
class AdminSettlementController(
    private val adminSettlementService: AdminSettlementService,
) {

    @GetMapping("/dashboard")
    @Operation(summary = "운영자 정산 현황 대시보드 조회")
    fun getDashboard(
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<AdminSettlementDashboardResponse>> =
        ResponseEntity.ok(ApiResponse.success(adminSettlementService.getDashboard(year, month)))

    @GetMapping
    @Operation(summary = "운영자 정산 현황 목록 조회")
    fun getSettlements(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(defaultValue = "ALL") status: AdminSettlementStatusFilter,
        pageable: Pageable,
    ): ResponseEntity<ApiResponse<AdminSettlementPageResponse>> =
        ResponseEntity.ok(ApiResponse.success(adminSettlementService.getSettlements(year, month, status, pageable)))

    @GetMapping("/{settlementId}")
    @Operation(summary = "운영자 정산 현황 상세 조회")
    fun getSettlementDetail(
        @PathVariable settlementId: Long,
    ): ResponseEntity<ApiResponse<AdminSettlementDetailResponse>> =
        ResponseEntity.ok(ApiResponse.success(adminSettlementService.getSettlementDetail(settlementId)))
}
