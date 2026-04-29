package com.moongchijang.presentation.groupbuy

import com.moongchijang.application.groupbuy.GroupBuyRequestService
import com.moongchijang.application.groupbuy.dto.GroupBuyRequestCreateRequest
import com.moongchijang.application.groupbuy.dto.GroupBuyRequestIdResponse
import com.moongchijang.application.groupbuy.dto.GroupBuyRequestResponse
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: GroupBuyRequestCreateRequest
    ): ResponseEntity<ApiResponse<GroupBuyRequestIdResponse>> {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(groupBuyRequestService.create(userId, request)))
    }

    @GetMapping
    @Operation(summary = "내 공구 요청 목록 조회")
    fun getMyRequests(
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<ApiResponse<List<GroupBuyRequestResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(groupBuyRequestService.getMyRequests(userId)))
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "공구 요청 상세 조회")
    fun getDetail(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<GroupBuyRequestResponse>> {
        return ResponseEntity.ok(ApiResponse.success(groupBuyRequestService.getDetail(userId, requestId)))
    }
}
