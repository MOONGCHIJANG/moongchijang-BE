package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminRefundRequestService
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestApproveRequest
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestCaseFilter
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestDetailResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestPageResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestRejectRequest
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestTab
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.authorization.RequireCurrentRole
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/refund-requests")
@RequireCurrentRole(UserRole.ADMIN)
@Tag(name = "Admin", description = "운영자 관리")
class AdminRefundRequestController(
    private val adminRefundRequestService: AdminRefundRequestService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "운영자 환불 요청 목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "환불 요청 목록 조회 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요"),
            SwaggerApiResponse(responseCode = "403", description = "접근 권한 없음"),
        ]
    )
    fun getRefundRequests(
        @RequestParam(defaultValue = "ALL") tab: AdminRefundRequestTab,
        @RequestParam(defaultValue = "ALL") caseFilter: AdminRefundRequestCaseFilter,
        @RequestParam(required = false) keyword: String?,
        pageable: Pageable,
    ): ResponseEntity<ApiResponse<AdminRefundRequestPageResponse>> {
        log.info(
            "[AdminRefundRequestController] 환불 요청 목록 조회 요청: tab={}, caseFilter={}, keyword={}, page={}, size={}",
            tab, caseFilter, keyword, pageable.pageNumber, pageable.pageSize
        )
        return ResponseEntity.ok(
            ApiResponse.success(
                adminRefundRequestService.getRefundRequests(
                    tab = tab,
                    caseFilter = caseFilter,
                    keyword = keyword,
                    pageable = pageable,
                )
            )
        )
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "운영자 환불 요청 상세 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "환불 요청 상세 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요"),
            SwaggerApiResponse(responseCode = "403", description = "접근 권한 없음"),
            SwaggerApiResponse(responseCode = "404", description = "환불 요청을 찾을 수 없음"),
        ]
    )
    fun getRefundRequestDetail(
        @PathVariable requestId: Long,
    ): ResponseEntity<ApiResponse<AdminRefundRequestDetailResponse>> {
        log.info("[AdminRefundRequestController] 환불 요청 상세 조회 요청: requestId={}", requestId)
        return ResponseEntity.ok(ApiResponse.success(adminRefundRequestService.getRefundRequestDetail(requestId)))
    }

    @PatchMapping("/{requestId}/approve")
    @Operation(summary = "운영자 환불 요청 승인 처리")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "환불 요청 승인 처리 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 환불 금액 또는 상태 전이"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요"),
            SwaggerApiResponse(responseCode = "403", description = "접근 권한 없음"),
            SwaggerApiResponse(responseCode = "404", description = "환불 요청을 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "409", description = "이미 처리된 환불 요청"),
        ]
    )
    fun approveRefundRequest(
        @PathVariable requestId: Long,
        @Valid @RequestBody request: AdminRefundRequestApproveRequest,
    ): ResponseEntity<ApiResponse<AdminRefundRequestDetailResponse>> {
        log.info(
            "[AdminRefundRequestController] 환불 요청 승인 처리 요청: requestId={}, refundAmount={}",
            requestId,
            request.refundAmount
        )
        return ResponseEntity.ok(ApiResponse.success(adminRefundRequestService.approveRefundRequest(requestId, request)))
    }

    @PatchMapping("/{requestId}/reject")
    @Operation(summary = "운영자 환불 요청 거절 처리")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "환불 요청 거절 처리 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 거절 사유 또는 상태 전이"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요"),
            SwaggerApiResponse(responseCode = "403", description = "접근 권한 없음"),
            SwaggerApiResponse(responseCode = "404", description = "환불 요청을 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "409", description = "이미 처리된 환불 요청"),
        ]
    )
    fun rejectRefundRequest(
        @PathVariable requestId: Long,
        @Valid @RequestBody request: AdminRefundRequestRejectRequest,
    ): ResponseEntity<ApiResponse<AdminRefundRequestDetailResponse>> {
        log.info(
            "[AdminRefundRequestController] 환불 요청 거절 처리 요청: requestId={}, rejectionReasonLength={}",
            requestId,
            request.rejectionReason.trim().length
        )
        return ResponseEntity.ok(ApiResponse.success(adminRefundRequestService.rejectRefundRequest(requestId, request)))
    }
}
