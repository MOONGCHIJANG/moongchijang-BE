package com.moongchijang.presentation.groupbuy

import com.moongchijang.application.groupbuy.GroupBuyRequestService
import com.moongchijang.application.groupbuy.dto.GroupBuyRequestCreateRequest
import com.moongchijang.application.groupbuy.dto.GroupBuyRequestIdResponse
import com.moongchijang.application.groupbuy.dto.GroupBuyRequestResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/group-buy-requests")
@Tag(name = "GroupBuyRequest", description = "공구 개설 요청 (소비자)")
class GroupBuyRequestController(
    private val groupBuyRequestService: GroupBuyRequestService
) {

    @PostMapping
    @Operation(summary = "공구 개설 요청 제출")
    fun create(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: GroupBuyRequestCreateRequest
    ): ResponseEntity<ApiResponse<GroupBuyRequestIdResponse>> {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(groupBuyRequestService.create(principal.id, request)))
    }

    @GetMapping
    @Operation(summary = "내 공구 요청 목록 조회")
    fun getMyRequests(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<GroupBuyRequestResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(groupBuyRequestService.getMyRequests(principal.id)))
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "공구 요청 상세 조회")
    fun getDetail(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<GroupBuyRequestResponse>> {
        return ResponseEntity.ok(ApiResponse.success(groupBuyRequestService.getDetail(principal.id, requestId)))
    }
}
