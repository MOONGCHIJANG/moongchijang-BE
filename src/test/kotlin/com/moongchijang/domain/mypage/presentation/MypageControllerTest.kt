package com.moongchijang.domain.mypage.presentation

import com.moongchijang.domain.mypage.application.MypageService
import com.moongchijang.domain.mypage.application.dto.MypageParticipationStatusFilter
import com.moongchijang.domain.mypage.application.dto.MypageSummaryResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.security.principal.CustomUserPrincipal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MypageControllerTest {

    private val mypageService: MypageService = mock(MypageService::class.java)
    private val controller = MypageController(mypageService)

    @Test
    fun `users me participations는 ACTIVE 상태 필터를 서비스로 전달한다`() {
        `when`(mypageService.getParticipations(1L, MypageParticipationStatusFilter.ACTIVE)).thenReturn(emptyList())

        val result = controller.getUserParticipations(principal(), MypageParticipationStatusFilter.ACTIVE)

        assertEquals(emptyList<Any>(), result.body?.data)
        verify(mypageService).getParticipations(1L, MypageParticipationStatusFilter.ACTIVE)
    }

    @Test
    fun `users me participations는 COMPLETED 상태 필터를 서비스로 전달한다`() {
        `when`(mypageService.getParticipations(1L, MypageParticipationStatusFilter.COMPLETED)).thenReturn(emptyList())

        controller.getUserParticipations(principal(), MypageParticipationStatusFilter.COMPLETED)

        verify(mypageService).getParticipations(1L, MypageParticipationStatusFilter.COMPLETED)
    }

    @Test
    fun `users me participations는 REFUNDED 상태 필터를 서비스로 전달한다`() {
        `when`(mypageService.getParticipations(1L, MypageParticipationStatusFilter.REFUNDED)).thenReturn(emptyList())

        controller.getUserParticipations(principal(), MypageParticipationStatusFilter.REFUNDED)

        verify(mypageService).getParticipations(1L, MypageParticipationStatusFilter.REFUNDED)
    }

    @Test
    fun `users me tabs counts는 summary를 반환한다`() {
        val summary = MypageSummaryResponse(activeCount = 1, completedCount = 2, refundedCount = 3, requestCount = 4)
        `when`(mypageService.getSummary(1L)).thenReturn(summary)

        val result = controller.getUserTabCounts(principal())

        assertEquals(summary, result.body?.data)
        verify(mypageService).getSummary(1L)
    }

    @Test
    fun `users me group buy requests는 내 개설 요청 목록을 반환한다`() {
        `when`(mypageService.getGroupBuyRequests(1L)).thenReturn(emptyList())

        val result = controller.getUserGroupBuyRequests(principal())

        assertEquals(emptyList<Any>(), result.body?.data)
        verify(mypageService).getGroupBuyRequests(1L)
    }

    @Test
    fun `users me refunds는 마이페이지 환불 목록을 반환한다`() {
        `when`(mypageService.getRefunds(1L)).thenReturn(emptyList())

        val result = controller.getUserRefunds(principal())

        assertEquals(emptyList<Any>(), result.body?.data)
        verify(mypageService).getRefunds(1L)
    }

    private fun principal(): CustomUserPrincipal =
        CustomUserPrincipal(
            id = 1L,
            email = "user@example.com",
            role = UserRole.BUYER,
        )
}
