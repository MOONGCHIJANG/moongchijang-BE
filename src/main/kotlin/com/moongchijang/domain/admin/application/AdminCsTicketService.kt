package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketDetailResponse
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketPageResponse
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketStatusFilter
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketUpdateRequest
import com.moongchijang.domain.csticket.domain.repository.CsTicketRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.time.kstNow
import com.moongchijang.security.crypto.PersonalInfoManager
import org.slf4j.LoggerFactory
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
    private val personalInfoManager: PersonalInfoManager,
) {
    private val log = LoggerFactory.getLogger(AdminCsTicketService::class.java)

    fun getTickets(
        status: AdminCsTicketStatusFilter,
        keyword: String?,
        pageable: Pageable,
    ): AdminCsTicketPageResponse {
        log.info(
            "[AdminCsTicketService] CS 티켓 목록 조회 시작: status={}, page={}, size={}",
            status,
            pageable.pageNumber,
            pageable.pageSize,
        )
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }
        val page = csTicketRepository.findAdminPage(
            status = status.toStatus(),
            keyword = normalizedKeyword,
            ticketId = normalizedKeyword?.toLongOrNull(),
            pageable = pageable
        )

        val response = AdminCsTicketPageResponse.from(page, clock.kstNow())
        log.info("[AdminCsTicketService] CS 티켓 목록 조회 완료: status={}, totalElements={}", status, response.totalElements)
        return response
    }

    fun getTicketDetail(ticketId: Long): AdminCsTicketDetailResponse {
        log.info("[AdminCsTicketService] CS 티켓 상세 조회 시작: ticketId={}", ticketId)
        val ticket = csTicketRepository.findAdminDetailById(ticketId)
            .orElseThrow { CustomException(ErrorCode.CS_TICKET_NOT_FOUND) }

        val response = AdminCsTicketDetailResponse.from(
            ticket,
            clock.kstNow(),
            consumerEmail = personalInfoManager.decryptIfNeeded(ticket.consumer?.email),
            consumerPhoneNumber = personalInfoManager.decryptIfNeeded(ticket.consumer?.phoneNumber),
        )
        log.info("[AdminCsTicketService] CS 티켓 상세 조회 완료: ticketId={}", ticketId)
        return response
    }

    @Transactional
    fun updateTicket(
        ticketId: Long,
        request: AdminCsTicketUpdateRequest,
    ): AdminCsTicketDetailResponse {
        log.info("[AdminCsTicketService] CS 티켓 수정 시작: ticketId={}, status={}", ticketId, request.status)
        val now = clock.kstNow()
        val ticket = csTicketRepository.findAdminDetailById(ticketId)
            .orElseThrow { CustomException(ErrorCode.CS_TICKET_NOT_FOUND) }

        ticket.updateProcessing(
            status = request.status,
            assigneeName = request.assigneeName,
            processingMemo = request.processingMemo,
            now = now
        )

        val response = AdminCsTicketDetailResponse.from(
            ticket,
            now,
            consumerEmail = personalInfoManager.decryptIfNeeded(ticket.consumer?.email),
            consumerPhoneNumber = personalInfoManager.decryptIfNeeded(ticket.consumer?.phoneNumber),
        )
        log.info("[AdminCsTicketService] CS 티켓 수정 완료: ticketId={}, status={}", ticketId, response.status)
        return response
    }
}
