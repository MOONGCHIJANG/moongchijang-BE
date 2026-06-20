package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestCaseFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
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
import java.time.ZoneOffset

class AdminDashboardUrgentRefundServiceTest {

    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-28T01:00:00Z"), ZoneOffset.UTC)
    private val service = AdminDashboardUrgentRefundService(
        participationRepository = participationRepository,
        clock = clock,
    )

    @Test
    fun `1시간 초과 환불 요청을 경과 시간과 환불 예상 금액으로 반환한다`() {
        val pageable = PageRequest.of(0, 10)
        val requestedAt = LocalDateTime.of(2026, 5, 27, 22, 50)
        val participation = refundParticipation(id = 101L, cancelledAt = requestedAt)
        val requestedBefore = LocalDateTime.of(2026, 5, 28, 0, 0)
        `when`(
            participationRepository.findDashboardUrgentRefundRequests(
                status = ParticipationStatus.REFUND_PENDING,
                requestedBefore = requestedBefore,
                pageable = pageable,
            )
        ).thenReturn(PageImpl(listOf(participation), pageable, 1))

        val result = service.getUrgentRefunds(pageable)

        assertTrue(result.hasUrgentRefunds)
        assertEquals(1L, result.totalUrgentCount)
        assertEquals(1, result.content.size)
        assertEquals(101L, result.content.first().requestId)
        assertEquals(AdminRefundRequestCaseFilter.PICKUP_PERIOD_NO_SHOW, result.content.first().caseFilter)
        assertEquals("환불유저", result.content.first().consumerName)
        assertEquals("초코 크루아상", result.content.first().groupBuyName)
        assertEquals(21_600, result.content.first().refundAmount)
        assertEquals(2L, result.content.first().slaElapsedHours)
        verify(participationRepository).findDashboardUrgentRefundRequests(
            ParticipationStatus.REFUND_PENDING,
            requestedBefore,
            pageable,
        )
    }

    private fun refundParticipation(id: Long, cancelledAt: LocalDateTime): Participation {
        val user = UserFixture.createKakaoUser(id = 7L, nickname = "환불유저")
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 3001L,
            status = GroupBuyStatus.ACHIEVED,
            productName = "초코 크루아상",
            price = 24_000,
        )
        groupBuy.store.id = 11L

        return Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 21_600,
            feeAmount = 2_400,
            totalAmount = 24_000,
            status = ParticipationStatus.REFUND_PENDING,
            cancelReason = ParticipationCancelReason.TIME_UNAVAILABLE,
            cancelReasonDetail = "환불 요청",
            cancelledAt = cancelledAt,
            id = id,
        )
    }
}
