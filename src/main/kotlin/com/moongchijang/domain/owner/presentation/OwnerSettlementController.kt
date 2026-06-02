package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.OwnerSettlementService
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundRequestDetailResponse
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundRequestListResponse
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundRequestTab
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundReviewSubmitRequest
import com.moongchijang.domain.owner.application.dto.refund.OwnerRefundReviewSubmitResponse
import com.moongchijang.domain.owner.application.dto.settlement.OwnerSettlementItemListResponse
import com.moongchijang.domain.owner.application.dto.settlement.OwnerSettlementMonthChipListResponse
import com.moongchijang.domain.owner.application.dto.settlement.OwnerSettlementMonthlySummaryResponse
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/owner/settlements")
@Tag(name = "OwnerSettlement", description = "사장님 정산/환불 확인")
class OwnerSettlementController(
    private val ownerSettlementService: OwnerSettlementService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/monthly-summary")
    @Operation(summary = "사장님 월별 정산 예정 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "400", description = "요청 파라미터 오류", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun getMonthlySummary(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<OwnerSettlementMonthlySummaryResponse>> {
        log.info("[OwnerSettlementController] 월별 정산 예정 조회 요청 수신: ownerId={}, year={}, month={}", principal.id, year, month)
        val response = ownerSettlementService.getMonthlySettlementSummary(principal.id, year, month)
        log.info("[OwnerSettlementController] 월별 정산 예정 조회 응답 완료: ownerId={}, year={}, month={}", principal.id, year, month)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/month-chips")
    @Operation(summary = "사장님 정산 월 칩 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun getMonthChips(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
    ): ResponseEntity<ApiResponse<OwnerSettlementMonthChipListResponse>> {
        log.info("[OwnerSettlementController] 정산 월 칩 조회 요청 수신: ownerId={}", principal.id)
        val response = ownerSettlementService.getSettlementMonthChips(principal.id)
        log.info("[OwnerSettlementController] 정산 월 칩 조회 응답 완료: ownerId={}, count={}", principal.id, response.chips.size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/items")
    @Operation(summary = "사장님 정산 공구 카드 목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "400", description = "요청 파라미터 오류", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun getSettlementItems(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<OwnerSettlementItemListResponse>> {
        log.info("[OwnerSettlementController] 정산 공구 카드 목록 조회 요청 수신: ownerId={}, year={}, month={}", principal.id, year, month)
        val response = ownerSettlementService.getSettlementItems(principal.id, year, month)
        log.info("[OwnerSettlementController] 정산 공구 카드 목록 조회 응답 완료: ownerId={}, year={}, month={}, count={}", principal.id, year, month, response.items.size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/refund-requests")
    @Operation(summary = "사장님 환불 요청 목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun getRefundRequests(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestParam(required = false, defaultValue = "ALL") tab: OwnerRefundRequestTab,
    ): ResponseEntity<ApiResponse<OwnerRefundRequestListResponse>> {
        log.info("[OwnerSettlementController] 환불 요청 목록 조회 요청 수신: ownerId={}, tab={}", principal.id, tab)
        val response = ownerSettlementService.getRefundRequests(principal.id, tab)
        log.info("[OwnerSettlementController] 환불 요청 목록 조회 응답 완료: ownerId={}, tab={}, count={}", principal.id, tab, response.items.size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/refund-requests/{participationId}")
    @Operation(summary = "사장님 환불 요청 상세 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "대상 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun getRefundRequestDetail(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable participationId: Long,
    ): ResponseEntity<ApiResponse<OwnerRefundRequestDetailResponse>> {
        log.info("[OwnerSettlementController] 환불 요청 상세 조회 요청 수신: ownerId={}, participationId={}", principal.id, participationId)
        val response = ownerSettlementService.getRefundRequestDetail(principal.id, participationId)
        log.info("[OwnerSettlementController] 환불 요청 상세 조회 응답 완료: ownerId={}, participationId={}", principal.id, participationId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/refund-requests/{participationId}/review-submissions")
    @Operation(summary = "사장님 환불 요청 검토 제출")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "제출 성공"),
            SwaggerApiResponse(responseCode = "400", description = "요청값/상태 오류", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "대상 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun submitRefundReview(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable participationId: Long,
        @Valid @RequestBody request: OwnerRefundReviewSubmitRequest,
    ): ResponseEntity<ApiResponse<OwnerRefundReviewSubmitResponse>> {
        log.info(
            "[OwnerSettlementController] 환불 요청 검토 제출 요청 수신: ownerId={}, participationId={}, action={}",
            principal.id,
            participationId,
            request.action,
        )
        val response = ownerSettlementService.submitRefundReview(principal.id, participationId, request)
        log.info(
            "[OwnerSettlementController] 환불 요청 검토 제출 응답 완료: ownerId={}, participationId={}, action={}",
            principal.id,
            participationId,
            request.action,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
