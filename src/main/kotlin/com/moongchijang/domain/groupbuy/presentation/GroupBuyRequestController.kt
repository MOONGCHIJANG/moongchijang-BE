package com.moongchijang.domain.groupbuy.presentation

import com.moongchijang.domain.groupbuy.application.GroupBuyRequestService
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestCreateRequest
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestIdResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(GroupBuyRequestController::class.java)

    @PostMapping
    @Operation(summary = "공구 개설 요청 제출")
    fun create(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: GroupBuyRequestCreateRequest
    ): ResponseEntity<ApiResponse<GroupBuyRequestIdResponse>> {
        log.info("[GroupBuyRequestController] 공구 개설 요청 제출 요청: userId={}", principal.id)
        val response = ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(groupBuyRequestService.create(principal.id, request)))
        log.info("[GroupBuyRequestController] 공구 개설 요청 제출 응답 완료: userId={}", principal.id)
        return response
    }

    @GetMapping
    @Operation(summary = "내 공구 요청 목록 조회")
    fun getMyRequests(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<GroupBuyRequestResponse>>> {
        log.info("[GroupBuyRequestController] 내 공구 요청 목록 조회 요청: userId={}", principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(groupBuyRequestService.getMyRequests(principal.id)))
        log.info("[GroupBuyRequestController] 내 공구 요청 목록 조회 응답 완료: userId={}", principal.id)
        return response
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "공구 요청 상세 조회")
    fun getDetail(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<GroupBuyRequestResponse>> {
        log.info("[GroupBuyRequestController] 공구 요청 상세 조회 요청: userId={}, requestId={}", principal.id, requestId)
        val response = ResponseEntity.ok(ApiResponse.success(groupBuyRequestService.getDetail(principal.id, requestId)))
        log.info("[GroupBuyRequestController] 공구 요청 상세 조회 응답 완료: userId={}, requestId={}", principal.id, requestId)
        return response
    }
}
