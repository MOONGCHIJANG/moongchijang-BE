package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminSettlementService
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementDashboardResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementDetailResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementPageResponse
import com.moongchijang.domain.admin.application.dto.settlement.AdminSettlementStatusFilter
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(AdminSettlementController::class.java)

    @GetMapping("/dashboard")
    @Operation(summary = "운영자 정산 현황 대시보드 조회")
    fun getDashboard(
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<AdminSettlementDashboardResponse>> {
        log.info("[AdminSettlementController] 정산 대시보드 조회 요청: year={}, month={}", year, month)
        val response = ResponseEntity.ok(ApiResponse.success(adminSettlementService.getDashboard(year, month)))
        log.info("[AdminSettlementController] 정산 대시보드 조회 응답 완료: year={}, month={}", year, month)
        return response
    }

    @GetMapping
    @Operation(summary = "운영자 정산 현황 목록 조회")
    fun getSettlements(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(defaultValue = "ALL") status: AdminSettlementStatusFilter,
        pageable: Pageable,
    ): ResponseEntity<ApiResponse<AdminSettlementPageResponse>> {
        log.info(
            "[AdminSettlementController] 정산 목록 조회 요청: year={}, month={}, status={}, page={}, size={}",
            year,
            month,
            status,
            pageable.pageNumber,
            pageable.pageSize,
        )
        val response = ResponseEntity.ok(ApiResponse.success(adminSettlementService.getSettlements(year, month, status, pageable)))
        log.info("[AdminSettlementController] 정산 목록 조회 응답 완료: year={}, month={}, status={}", year, month, status)
        return response
    }

    @GetMapping("/{settlementId}")
    @Operation(summary = "운영자 정산 현황 상세 조회")
    fun getSettlementDetail(
        @PathVariable settlementId: Long,
    ): ResponseEntity<ApiResponse<AdminSettlementDetailResponse>> {
        log.info("[AdminSettlementController] 정산 상세 조회 요청: settlementId={}", settlementId)
        val response = ResponseEntity.ok(ApiResponse.success(adminSettlementService.getSettlementDetail(settlementId)))
        log.info("[AdminSettlementController] 정산 상세 조회 응답 완료: settlementId={}", settlementId)
        return response
    }
}
