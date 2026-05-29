package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminOrderService
import com.moongchijang.domain.admin.application.dto.AdminOrderDetailResponse
import com.moongchijang.domain.admin.application.dto.AdminOrderPageResponse
import com.moongchijang.domain.admin.application.dto.AdminOrderStatusFilter
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin", description = "운영자 관리")
class AdminOrderController(
    private val adminOrderService: AdminOrderService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "운영자 발주 관리 목록 조회")
    fun getOrders(
        @RequestParam(defaultValue = "ALL") status: AdminOrderStatusFilter,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<AdminOrderPageResponse>> {
        log.info("[AdminOrderController] 운영자 발주 목록 조회 요청: status={}, page={}, size={}", status, pageable.pageNumber, pageable.pageSize)
        val response = ResponseEntity.ok(ApiResponse.success(adminOrderService.getOrders(status, pageable)))
        log.info("[AdminOrderController] 운영자 발주 목록 조회 응답 완료: status={}", status)
        return response
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "운영자 발주 관리 상세 조회")
    fun getOrderDetail(
        @PathVariable orderId: Long
    ): ResponseEntity<ApiResponse<AdminOrderDetailResponse>> {
        log.info("[AdminOrderController] 운영자 발주 상세 조회 요청: orderId={}", orderId)
        val response = ResponseEntity.ok(ApiResponse.success(adminOrderService.getOrderDetail(orderId)))
        log.info("[AdminOrderController] 운영자 발주 상세 조회 응답 완료: orderId={}", orderId)
        return response
    }

    @PostMapping("/{orderId}/owner-contact")
    @Operation(summary = "운영자 발주 사장님 연락 완료 기록")
    fun markOwnerContacted(
        @PathVariable orderId: Long
    ): ResponseEntity<ApiResponse<AdminOrderDetailResponse>> {
        log.info("[AdminOrderController] 운영자 발주 사장님 연락 완료 처리 요청: orderId={}", orderId)
        val response = ResponseEntity.ok(ApiResponse.success(adminOrderService.markOwnerContacted(orderId)))
        log.info("[AdminOrderController] 운영자 발주 사장님 연락 완료 처리 응답 완료: orderId={}", orderId)
        return response
    }

    @PostMapping("/{orderId}/confirm")
    @Operation(summary = "운영자 발주 확정 처리")
    fun confirmOrder(
        @PathVariable orderId: Long
    ): ResponseEntity<ApiResponse<AdminOrderDetailResponse>> {
        log.info("[AdminOrderController] 운영자 발주 확정 처리 요청: orderId={}", orderId)
        val response = ResponseEntity.ok(ApiResponse.success(adminOrderService.confirmOrder(orderId)))
        log.info("[AdminOrderController] 운영자 발주 확정 처리 응답 완료: orderId={}", orderId)
        return response
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "운영자 발주 취소 처리")
    fun cancelOrder(
        @PathVariable orderId: Long
    ): ResponseEntity<ApiResponse<AdminOrderDetailResponse>> {
        log.info("[AdminOrderController] 운영자 발주 취소 처리 요청: orderId={}", orderId)
        val response = ResponseEntity.ok(ApiResponse.success(adminOrderService.cancelOrder(orderId)))
        log.info("[AdminOrderController] 운영자 발주 취소 처리 응답 완료: orderId={}", orderId)
        return response
    }
}
