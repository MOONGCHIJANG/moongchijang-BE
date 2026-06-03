package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminOwnerGroupBuyRequestService
import com.moongchijang.domain.admin.application.dto.AdminOwnerGroupBuyRequestActionResponse
import com.moongchijang.domain.admin.application.dto.AdminOwnerGroupBuyRequestDetailResponse
import com.moongchijang.domain.admin.application.dto.AdminOwnerGroupBuyRequestPageResponse
import com.moongchijang.domain.admin.application.dto.AdminOwnerGroupBuyRequestRejectRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/owner-group-buy-requests")
@Tag(name = "AdminOwnerGroupBuyRequest", description = "어드민 사장님 공구 개설 요청 관리")
class AdminOwnerGroupBuyRequestController(
    private val adminOwnerGroupBuyRequestService: AdminOwnerGroupBuyRequestService
) {

    @GetMapping
    @Operation(summary = "사장님 공구 개설 요청 목록 조회")
    fun getRequests(
        @RequestParam(required = false) status: OwnerGroupBuyRequestStatus?,
        @RequestParam(required = false) keyword: String?,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<AdminOwnerGroupBuyRequestPageResponse>> {
        val response = ResponseEntity.ok(
            ApiResponse.success(adminOwnerGroupBuyRequestService.getRequests(status, keyword, pageable))
        )
        return response
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "사장님 공구 개설 요청 상세 조회")
    fun getDetail(
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<AdminOwnerGroupBuyRequestDetailResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(adminOwnerGroupBuyRequestService.getDetail(requestId)))
        return response
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "사장님 공구 개설 요청 승인 및 공구 생성")
    fun approve(
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<AdminOwnerGroupBuyRequestActionResponse>> {
        val response = ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(adminOwnerGroupBuyRequestService.approve(requestId)))
        return response
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "사장님 공구 개설 요청 반려")
    fun reject(
        @PathVariable requestId: Long,
        @Valid @RequestBody request: AdminOwnerGroupBuyRequestRejectRequest
    ): ResponseEntity<ApiResponse<AdminOwnerGroupBuyRequestActionResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(adminOwnerGroupBuyRequestService.reject(requestId, request)))
        return response
    }
}
