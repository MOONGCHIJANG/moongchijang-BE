package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminRefundRequestService
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestPageResponse
import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestTab
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/refund-requests")
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
        pageable: Pageable,
    ): ResponseEntity<ApiResponse<AdminRefundRequestPageResponse>> {
        log.info(
            "[AdminRefundRequestController] 환불 요청 목록 조회 요청: tab={}, page={}, size={}",
            tab, pageable.pageNumber, pageable.pageSize
        )
        return ResponseEntity.ok(ApiResponse.success(adminRefundRequestService.getRefundRequests(tab, pageable)))
    }
}
