package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminCsTicketService
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketDetailResponse
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketPageResponse
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketStatusFilter
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketUpdateRequest
import com.moongchijang.domain.csticket.domain.entity.CsTicketPriority
import com.moongchijang.domain.csticket.domain.entity.CsTicketStatus
import com.moongchijang.domain.csticket.domain.entity.CsTicketType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class AdminCsTicketControllerTest {

    private val adminCsTicketService: AdminCsTicketService = mock(AdminCsTicketService::class.java)
    private val controller = AdminCsTicketController(adminCsTicketService)

    @Test
    fun `운영자 CS 티켓 목록 조회는 상태 검색어 페이징을 서비스로 전달한다`() {
        val pageable = PageRequest.of(0, 20)
        val response = AdminCsTicketPageResponse(
            content = emptyList(),
            totalElements = 0,
            totalPages = 0,
            number = 0,
            size = 20
        )
        `when`(
            adminCsTicketService.getTickets(
                AdminCsTicketStatusFilter.IN_PROGRESS,
                "소비자",
                pageable
            )
        ).thenReturn(response)

        val result = controller.getTickets(AdminCsTicketStatusFilter.IN_PROGRESS, "소비자", pageable)

        assertEquals(response, result.body?.data)
        verify(adminCsTicketService).getTickets(AdminCsTicketStatusFilter.IN_PROGRESS, "소비자", pageable)
    }

    @Test
    fun `운영자 CS 티켓 상세 조회는 티켓 ID를 서비스로 전달한다`() {
        val response = detailResponse(ticketId = 10L)
        `when`(adminCsTicketService.getTicketDetail(10L)).thenReturn(response)

        val result = controller.getTicketDetail(10L)

        assertEquals(response, result.body?.data)
        verify(adminCsTicketService).getTicketDetail(10L)
    }

    @Test
    fun `운영자 CS 티켓 변경은 티켓 ID와 요청을 서비스로 전달한다`() {
        val request = AdminCsTicketUpdateRequest(
            status = CsTicketStatus.COMPLETED,
            assigneeName = "김은서",
            processingMemo = "처리 완료"
        )
        val response = detailResponse(ticketId = 11L, status = CsTicketStatus.COMPLETED)
        `when`(adminCsTicketService.updateTicket(11L, request)).thenReturn(response)

        val result = controller.updateTicket(11L, request)

        assertEquals(response, result.body?.data)
        verify(adminCsTicketService).updateTicket(11L, request)
    }

    private fun detailResponse(
        ticketId: Long,
        status: CsTicketStatus = CsTicketStatus.RECEIVED,
    ): AdminCsTicketDetailResponse =
        AdminCsTicketDetailResponse(
            ticketId = ticketId,
            type = CsTicketType.PICKUP,
            title = "픽업 일정 문의",
            description = "픽업 시간을 바꿀 수 있나요?",
            priority = CsTicketPriority.MEDIUM,
            status = status,
            createdAt = LocalDateTime.of(2026, 5, 28, 10, 0),
            updatedAt = LocalDateTime.of(2026, 5, 28, 10, 0),
            slaHours = 3,
            consumer = null,
            owner = null,
            groupBuy = null,
            refundParticipationId = null,
            assigneeName = "김은서",
            processingMemo = "처리 완료",
            resolvedAt = null,
            actionable = status != CsTicketStatus.COMPLETED
        )
}
