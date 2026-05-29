package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminCsTicketService
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketDetailResponse
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketPageResponse
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketStatusFilter
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketUpdateRequest
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/api/v1/admin/cs-tickets")
@Tag(name = "Admin", description = "운영자 관리")
class AdminCsTicketController(
    private val adminCsTicketService: AdminCsTicketService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "운영자 CS 티켓 목록 조회")
    fun getTickets(
        @RequestParam(defaultValue = "ALL") status: AdminCsTicketStatusFilter,
        @RequestParam(required = false) keyword: String?,
        pageable: Pageable,
    ): ResponseEntity<ApiResponse<AdminCsTicketPageResponse>> {
        log.info("[AdminCsTicketController] CS 티켓 목록 조회 요청: status={}, page={}, size={}", status, pageable.pageNumber, pageable.pageSize)
        val response = ResponseEntity.ok(ApiResponse.success(adminCsTicketService.getTickets(status, keyword, pageable)))
        log.info("[AdminCsTicketController] CS 티켓 목록 조회 응답 완료: status={}", status)
        return response
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "운영자 CS 티켓 상세 조회")
    fun getTicketDetail(
        @PathVariable ticketId: Long,
    ): ResponseEntity<ApiResponse<AdminCsTicketDetailResponse>> {
        log.info("[AdminCsTicketController] CS 티켓 상세 조회 요청: ticketId={}", ticketId)
        val response = ResponseEntity.ok(ApiResponse.success(adminCsTicketService.getTicketDetail(ticketId)))
        log.info("[AdminCsTicketController] CS 티켓 상세 조회 응답 완료: ticketId={}", ticketId)
        return response
    }

    @PatchMapping("/{ticketId}")
    @Operation(summary = "운영자 CS 티켓 처리 정보 변경")
    fun updateTicket(
        @PathVariable ticketId: Long,
        @Valid @RequestBody request: AdminCsTicketUpdateRequest,
    ): ResponseEntity<ApiResponse<AdminCsTicketDetailResponse>> {
        log.info("[AdminCsTicketController] CS 티켓 수정 요청: ticketId={}, status={}", ticketId, request.status)
        val response = ResponseEntity.ok(ApiResponse.success(adminCsTicketService.updateTicket(ticketId, request)))
        log.info("[AdminCsTicketController] CS 티켓 수정 응답 완료: ticketId={}", ticketId)
        return response
    }
}
