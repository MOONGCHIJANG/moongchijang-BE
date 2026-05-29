package com.moongchijang.domain.groupbuy.presentation

import com.moongchijang.domain.groupbuy.application.AdminGroupBuyRequestActionService
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestActionResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestApproveRequest
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestRejectRequest
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/group-buy-requests")
@Tag(name = "GroupBuyRequestAdmin", description = "공구 개설 요청 관리")
class AdminGroupBuyRequestActionController(
    private val adminGroupBuyRequestActionService: AdminGroupBuyRequestActionService
) {

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "소비자 공구 개설 요청 승인 및 공구 생성")
    fun approve(
        @PathVariable requestId: Long,
        @Valid @RequestBody request: AdminGroupBuyRequestApproveRequest
    ): ResponseEntity<ApiResponse<AdminGroupBuyRequestActionResponse>> {
        val response = ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(adminGroupBuyRequestActionService.approve(requestId, request)))
        return response
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "소비자 공구 개설 요청 반려")
    fun reject(
        @PathVariable requestId: Long,
        @Valid @RequestBody request: AdminGroupBuyRequestRejectRequest
    ): ResponseEntity<ApiResponse<AdminGroupBuyRequestActionResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(adminGroupBuyRequestActionService.reject(requestId, request)))
        return response
    }
}
