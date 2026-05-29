package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.OwnerGroupBuyRequestService
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestDetailResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestPageResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/owner/group-buy-requests")
@Tag(name = "OwnerGroupBuyRequest", description = "사장님 공구 개설 요청")
class OwnerGroupBuyRequestController(
    private val ownerGroupBuyRequestService: OwnerGroupBuyRequestService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "사장님 공구 개설 요청 목록 조회")
    fun getMyRequests(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<OwnerGroupBuyRequestPageResponse>> {
        log.info("[OwnerGroupBuyRequestController] 사장님 공구요청 목록 조회 요청: ownerId={}, page={}, size={}", principal.id, pageable.pageNumber, pageable.pageSize)
        val response = ResponseEntity.ok(ApiResponse.success(ownerGroupBuyRequestService.getMyRequests(principal.id, pageable)))
        log.info("[OwnerGroupBuyRequestController] 사장님 공구요청 목록 조회 응답 완료: ownerId={}", principal.id)
        return response
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "사장님 공구 개설 요청 상세 조회")
    fun getDetail(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable requestId: Long
    ): ResponseEntity<ApiResponse<OwnerGroupBuyRequestDetailResponse>> {
        log.info("[OwnerGroupBuyRequestController] 사장님 공구요청 상세 조회 요청: ownerId={}, requestId={}", principal.id, requestId)
        val response = ResponseEntity.ok(ApiResponse.success(ownerGroupBuyRequestService.getDetail(principal.id, requestId)))
        log.info("[OwnerGroupBuyRequestController] 사장님 공구요청 상세 조회 응답 완료: ownerId={}, requestId={}", principal.id, requestId)
        return response
    }

    @PostMapping
    @Operation(summary = "사장님 공구 개설 요청 제출")
    fun create(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: OwnerGroupBuyRequestCreateRequest
    ): ResponseEntity<ApiResponse<OwnerGroupBuyRequestCreateResponse>> {
        log.info("[OwnerGroupBuyRequestController] 사장님 공구요청 생성 요청: ownerId={}", principal.id)
        val response = ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(ownerGroupBuyRequestService.create(principal.id, request)))
        log.info("[OwnerGroupBuyRequestController] 사장님 공구요청 생성 응답 완료: ownerId={}", principal.id)
        return response
    }
}
