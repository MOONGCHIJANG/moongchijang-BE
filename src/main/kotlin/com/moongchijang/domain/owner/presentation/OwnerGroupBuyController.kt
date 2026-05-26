package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.OwnerGroupBuyService
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuySummaryResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/owner/group-buys")
@Tag(name = "OwnerGroupBuy", description = "사장님 공구 조회")
class OwnerGroupBuyController(
    private val ownerGroupBuyService: OwnerGroupBuyService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/summary")
    @Operation(summary = "사장님 공구 요약 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "사장님 공구 요약 조회 성공"),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "사장님 권한 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun getMyGroupBuySummary(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<OwnerGroupBuySummaryResponse>> {
        log.info("[OwnerGroupBuyController] 사장님 공구 요약 조회 요청 수신: ownerId={}", principal.id)
        val response = ownerGroupBuyService.getMyGroupBuySummary(principal.id)
        log.info("[OwnerGroupBuyController] 사장님 공구 요약 조회 응답 완료: ownerId={}, isEmpty={}", principal.id, response.isEmpty)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping
    @Operation(summary = "사장님 진행 중인 공구 목록 조회")
    fun getMyGroupBuys(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<OwnerGroupBuyListItemResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(ownerGroupBuyService.getMyGroupBuys(principal.id)))
    }
}
