package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminOrderService
import com.moongchijang.domain.admin.application.dto.AdminOrderPageResponse
import com.moongchijang.domain.admin.application.dto.AdminOrderStatusFilter
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin", description = "운영자 관리")
class AdminOrderController(
    private val adminOrderService: AdminOrderService,
) {

    @GetMapping
    @Operation(summary = "운영자 발주 관리 목록 조회")
    fun getOrders(
        @RequestParam(defaultValue = "ALL") status: AdminOrderStatusFilter,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<AdminOrderPageResponse>> =
        ResponseEntity.ok(ApiResponse.success(adminOrderService.getOrders(status, pageable)))
}
