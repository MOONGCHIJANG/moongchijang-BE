package com.moongchijang.domain.groupbuy.presentation

import com.moongchijang.domain.groupbuy.application.GroupBuyRequestService
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestDetailResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestPageResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestStatusFilter
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestStatusUpdateRequest
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/group-buy-requests")
@Tag(name = "GroupBuyRequestAdmin", description = "공구 개설 요청 관리")
class GroupBuyRequestAdminController(
    private val groupBuyRequestService: GroupBuyRequestService
) {

    @GetMapping
    @Operation(summary = "운영자 공구 개설 요청 목록 조회")
    fun getRequests(
        @RequestParam(defaultValue = "ALL") status: AdminGroupBuyRequestStatusFilter,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<AdminGroupBuyRequestPageResponse>> {
        return ResponseEntity.ok(ApiResponse.success(groupBuyRequestService.getAdminRequests(status, pageable)))
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "운영자 공구 개설 요청 상세 조회")
    fun getDetail(
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<AdminGroupBuyRequestDetailResponse>> {
        return ResponseEntity.ok(ApiResponse.success(groupBuyRequestService.getAdminDetail(requestId)))
    }

    @PatchMapping("/{requestId}/status")
    @Operation(summary = "공구 개설 요청 상태 변경")
    fun updateStatus(
        @PathVariable requestId: Long,
        @Valid @RequestBody request: GroupBuyRequestStatusUpdateRequest
    ): ResponseEntity<ApiResponse<GroupBuyRequestResponse>> {
        return ResponseEntity.ok(ApiResponse.success(groupBuyRequestService.updateStatus(requestId, request)))
    }
}
