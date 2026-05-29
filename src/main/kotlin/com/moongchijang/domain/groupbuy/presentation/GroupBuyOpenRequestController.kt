package com.moongchijang.domain.groupbuy.presentation

import com.moongchijang.domain.groupbuy.application.GroupBuyOpenRequestService
import com.moongchijang.domain.groupbuy.application.dto.CreateGroupBuyOpenRequestRequest
import com.moongchijang.domain.groupbuy.application.dto.StoreRecommendationRequest
import com.moongchijang.domain.groupbuy.application.dto.StoreRecommendationResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/group-buy-open-requests")
@Tag(name = "GroupBuyOpenRequest", description = "공구 개설 알림 신청")
class GroupBuyOpenRequestController(
    private val openRequestService: GroupBuyOpenRequestService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @Operation(summary = "공구 개설 알림 신청")
    fun create(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: CreateGroupBuyOpenRequestRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.info("[GroupBuyOpenRequestController] 공구 개설 알림 신청 요청: userId={}", principal.id)
        openRequestService.create(principal.id, request)
        val response = ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success())
        log.info("[GroupBuyOpenRequestController] 공구 개설 알림 신청 응답 완료: userId={}", principal.id)
        return response
    }

    @PostMapping("/store-recommendations")
    @Operation(summary = "공구 개설 요청 매장 추천")
    fun recommendStores(
        @Valid @RequestBody request: StoreRecommendationRequest
    ): ResponseEntity<ApiResponse<StoreRecommendationResponse>> {
        log.info("[GroupBuyOpenRequestController] 공구 개설 요청 매장 추천 요청 수신")
        val response = ResponseEntity.ok(ApiResponse.success(openRequestService.recommendStores(request)))
        log.info("[GroupBuyOpenRequestController] 공구 개설 요청 매장 추천 응답 완료")
        return response
    }
}
