package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.OwnerGroupBuyService
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyCloseRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyExtensionRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageDetailResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageFilterType
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuySummaryResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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

    @GetMapping("/manage")
    @Operation(summary = "사장님 공구 관리 목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "사장님 공구 관리 목록 조회 성공"),
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
    fun getManageGroupBuys(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestParam(required = false, defaultValue = "ALL")
        filter: OwnerGroupBuyManageFilterType
    ): ResponseEntity<ApiResponse<List<OwnerGroupBuyManageListItemResponse>>> {
        log.info("[OwnerGroupBuyController] 사장님 공구 관리 목록 조회 요청 수신: ownerId={}, filter={}", principal.id, filter)
        val response = ownerGroupBuyService.getManageGroupBuys(principal.id, filter)
        log.info("[OwnerGroupBuyController] 사장님 공구 관리 목록 조회 응답 완료: ownerId={}, filter={}, count={}", principal.id, filter, response.size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{groupBuyId}/manage/in-progress")
    @Operation(summary = "사장님 모집중 공구 상세 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "사장님 모집중 공구 상세 조회 성공"),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "사장님 권한 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "공구를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun getInProgressGroupBuyDetail(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable groupBuyId: Long
    ): ResponseEntity<ApiResponse<OwnerGroupBuyManageDetailResponse>> {
        log.info("[OwnerGroupBuyController] 사장님 모집중 공구 상세 조회 요청 수신: ownerId={}, groupBuyId={}", principal.id, groupBuyId)
        val response = ownerGroupBuyService.getInProgressGroupBuyDetail(principal.id, groupBuyId)
        log.info("[OwnerGroupBuyController] 사장님 모집중 공구 상세 조회 응답 완료: ownerId={}, groupBuyId={}", principal.id, groupBuyId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{groupBuyId}/manage/achieved")
    @Operation(summary = "사장님 달성 공구 상세 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "사장님 달성 공구 상세 조회 성공"),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "사장님 권한 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "공구를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun getAchievedGroupBuyDetail(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable groupBuyId: Long
    ): ResponseEntity<ApiResponse<OwnerGroupBuyManageDetailResponse>> {
        log.info("[OwnerGroupBuyController] 사장님 달성 공구 상세 조회 요청 수신: ownerId={}, groupBuyId={}", principal.id, groupBuyId)
        val response = ownerGroupBuyService.getAchievedGroupBuyDetail(principal.id, groupBuyId)
        log.info("[OwnerGroupBuyController] 사장님 달성 공구 상세 조회 응답 완료: ownerId={}, groupBuyId={}", principal.id, groupBuyId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/{groupBuyId}/extension-requests")
    @Operation(summary = "사장님 공구 기간 연장 요청")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "사장님 공구 기간 연장 요청 성공"),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "사장님 권한 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "공구를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun requestGroupBuyExtension(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable groupBuyId: Long,
        @Valid @RequestBody request: OwnerGroupBuyExtensionRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.info("[OwnerGroupBuyController] 사장님 공구 기간 연장 요청 수신: ownerId={}, groupBuyId={}", principal.id, groupBuyId)
        ownerGroupBuyService.requestGroupBuyExtension(principal.id, groupBuyId, request)
        log.info("[OwnerGroupBuyController] 사장님 공구 기간 연장 요청 응답 완료: ownerId={}, groupBuyId={}", principal.id, groupBuyId)
        return ResponseEntity.ok(ApiResponse.success())
    }

    @PostMapping("/{groupBuyId}/close-requests")
    @Operation(summary = "사장님 공구 마감 요청")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "사장님 공구 마감 요청 성공"),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "사장님 권한 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "공구를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun requestGroupBuyClose(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable groupBuyId: Long,
        @Valid @RequestBody request: OwnerGroupBuyCloseRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        log.info("[OwnerGroupBuyController] 사장님 공구 마감 요청 수신: ownerId={}, groupBuyId={}, reason={}", principal.id, groupBuyId, request.reason)
        ownerGroupBuyService.requestGroupBuyClose(principal.id, groupBuyId, request)
        log.info("[OwnerGroupBuyController] 사장님 공구 마감 요청 응답 완료: ownerId={}, groupBuyId={}", principal.id, groupBuyId)
        return ResponseEntity.ok(ApiResponse.success())
    }
}
