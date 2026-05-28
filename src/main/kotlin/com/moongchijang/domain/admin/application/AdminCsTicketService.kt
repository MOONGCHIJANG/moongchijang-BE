package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketDetailResponse
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketPageResponse
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketStatusFilter
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketUpdateRequest
import com.moongchijang.domain.csticket.domain.repository.CsTicketRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AdminCsTicketService(
    private val csTicketRepository: CsTicketRepository,
    private val clock: Clock,
) {

    fun getTickets(
        status: AdminCsTicketStatusFilter,
        keyword: String?,
        pageable: Pageable,
    ): AdminCsTicketPageResponse {
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }
        val page = csTicketRepository.findAdminPage(
            status = status.toStatus(),
            keyword = normalizedKeyword,
            ticketId = normalizedKeyword?.toLongOrNull(),
            pageable = pageable
        )

        return AdminCsTicketPageResponse.from(page, LocalDateTime.now(clock))
    }

    fun getTicketDetail(ticketId: Long): AdminCsTicketDetailResponse {
        val ticket = csTicketRepository.findAdminDetailById(ticketId)
            .orElseThrow { CustomException(ErrorCode.CS_TICKET_NOT_FOUND) }

        return AdminCsTicketDetailResponse.from(ticket, LocalDateTime.now(clock))
    }

    @Transactional
    fun updateTicket(
        ticketId: Long,
        request: AdminCsTicketUpdateRequest,
    ): AdminCsTicketDetailResponse {
        val now = LocalDateTime.now(clock)
        val ticket = csTicketRepository.findWithLockById(ticketId)
            .orElseThrow { CustomException(ErrorCode.CS_TICKET_NOT_FOUND) }

        ticket.updateProcessing(
            status = request.status,
            assigneeName = request.assigneeName,
            processingMemo = request.processingMemo,
            now = now
        )

        return AdminCsTicketDetailResponse.from(ticket, now)
    }
}
