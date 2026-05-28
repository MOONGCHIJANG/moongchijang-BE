package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketStatusFilter
import com.moongchijang.domain.admin.application.dto.csticket.AdminCsTicketUpdateRequest
import com.moongchijang.domain.csticket.domain.entity.CsTicket
import com.moongchijang.domain.csticket.domain.entity.CsTicketPriority
import com.moongchijang.domain.csticket.domain.entity.CsTicketStatus
import com.moongchijang.domain.csticket.domain.entity.CsTicketType
import com.moongchijang.domain.csticket.domain.repository.CsTicketRepository
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.global.entity.BaseEntity
import com.moongchijang.global.exception.CustomException
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

class AdminCsTicketServiceTest {

    private val csTicketRepository: CsTicketRepository = mock(CsTicketRepository::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-28T04:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AdminCsTicketService(csTicketRepository, clock)

    @Test
    fun `CS 티켓 목록을 상태와 검색어로 조회한다`() {
        val pageable = PageRequest.of(0, 20)
        val ticket = createTicket(id = 10L, status = CsTicketStatus.RECEIVED).apply {
            setAuditTime(LocalDateTime.of(2026, 5, 28, 10, 0))
        }
        `when`(
            csTicketRepository.findAdminPage(
                CsTicketStatus.RECEIVED,
                "10",
                10L,
                pageable
            )
        ).thenReturn(PageImpl(listOf(ticket), pageable, 1))

        val result = service.getTickets(AdminCsTicketStatusFilter.RECEIVED, " 10 ", pageable)

        assertEquals(1, result.content.size)
        assertEquals(10L, result.content[0].ticketId)
        assertEquals("픽업 일정 문의", result.content[0].title)
        assertEquals("테스트유저", result.content[0].consumerName)
        assertEquals("두쫀쿠 1개", result.content[0].groupBuyName)
        assertEquals(3L, result.content[0].slaHours)
        assertTrue(result.content[0].actionable)
    }

    @Test
    fun `빈 검색어는 검색 조건 없이 전체 상태로 조회한다`() {
        val pageable = PageRequest.of(0, 20)
        `when`(csTicketRepository.findAdminPage(null, null, null, pageable))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = service.getTickets(AdminCsTicketStatusFilter.ALL, "   ", pageable)

        assertTrue(result.content.isEmpty())
        verify(csTicketRepository).findAdminPage(null, null, null, pageable)
    }

    @Test
    fun `CS 티켓 상세를 조회한다`() {
        val ticket = createTicket(id = 11L, status = CsTicketStatus.IN_PROGRESS).apply {
            assigneeName = "김은서"
            processingMemo = "사장님 확인 대기"
            setAuditTime(LocalDateTime.of(2026, 5, 28, 9, 0))
        }
        `when`(csTicketRepository.findAdminDetailById(11L)).thenReturn(Optional.of(ticket))

        val result = service.getTicketDetail(11L)

        assertEquals(11L, result.ticketId)
        assertEquals(CsTicketStatus.IN_PROGRESS, result.status)
        assertEquals("김은서", result.assigneeName)
        assertEquals("사장님 확인 대기", result.processingMemo)
        assertEquals("뭉치장 베이커리", result.owner?.storeName)
        assertEquals(4L, result.slaHours)
        assertTrue(result.actionable)
    }

    @Test
    fun `존재하지 않는 CS 티켓 상세는 예외를 던진다`() {
        `when`(csTicketRepository.findAdminDetailById(404L)).thenReturn(Optional.empty())

        assertThrows(CustomException::class.java) {
            service.getTicketDetail(404L)
        }
    }

    @Test
    fun `CS 티켓 처리 상태 담당자 메모를 변경한다`() {
        val ticket = createTicket(id = 12L, status = CsTicketStatus.RECEIVED)
        `when`(csTicketRepository.findWithLockById(12L)).thenReturn(Optional.of(ticket))

        val result = service.updateTicket(
            ticketId = 12L,
            request = AdminCsTicketUpdateRequest(
                status = CsTicketStatus.COMPLETED,
                assigneeName = "김은서",
                processingMemo = "안내 완료"
            )
        )

        assertEquals(CsTicketStatus.COMPLETED, result.status)
        assertEquals("김은서", result.assigneeName)
        assertEquals("안내 완료", result.processingMemo)
        assertEquals(LocalDateTime.of(2026, 5, 28, 13, 0), result.resolvedAt)
        assertFalse(result.actionable)
        assertNotNull(ticket.resolvedAt)
    }

    private fun createTicket(
        id: Long,
        status: CsTicketStatus,
    ): CsTicket =
        CsTicket(
            type = CsTicketType.PICKUP,
            title = "픽업 일정 문의",
            description = "픽업 시간을 바꿀 수 있나요?",
            priority = CsTicketPriority.MEDIUM,
            status = status,
            consumer = UserFixture.createKakaoUser(id = 3L),
            groupBuy = GroupBuyFixture.createGroupBuy(id = 30L, status = GroupBuyStatus.IN_PROGRESS),
            refundParticipationId = null,
            id = id
        )

    private fun BaseEntity.setAuditTime(value: LocalDateTime) {
        BaseEntity::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(this@setAuditTime, value)
        }
        BaseEntity::class.java.getDeclaredField("updatedAt").apply {
            isAccessible = true
            set(this@setAuditTime, value)
        }
    }
}
