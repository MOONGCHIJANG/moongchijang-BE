package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.AdminOwnerGroupBuyCloseRequestService
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyCloseRequestActionResponse
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyCloseRequestRejectRequest
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/owner-group-buys")
@Tag(name = "AdminOwnerGroupBuyCloseRequest", description = "어드민 사장님 공구 마감 요청 관리")
class AdminOwnerGroupBuyCloseRequestController(
    private val adminOwnerGroupBuyCloseRequestService: AdminOwnerGroupBuyCloseRequestService
) {

    @PostMapping("/{groupBuyId}/close-requests/approve")
    @Operation(summary = "사장님 공구 마감 요청 승인")
    fun approve(
        @PathVariable groupBuyId: Long
    ): ResponseEntity<ApiResponse<AdminOwnerGroupBuyCloseRequestActionResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(adminOwnerGroupBuyCloseRequestService.approve(groupBuyId)))
        return response
    }

    @PostMapping("/{groupBuyId}/close-requests/reject")
    @Operation(summary = "사장님 공구 마감 요청 반려")
    fun reject(
        @PathVariable groupBuyId: Long,
        @Valid @RequestBody request: AdminOwnerGroupBuyCloseRequestRejectRequest
    ): ResponseEntity<ApiResponse<AdminOwnerGroupBuyCloseRequestActionResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(adminOwnerGroupBuyCloseRequestService.reject(groupBuyId, request)))
        return response
    }
}
