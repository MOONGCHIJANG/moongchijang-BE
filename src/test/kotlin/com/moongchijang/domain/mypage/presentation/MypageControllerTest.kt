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
    fun `users me participations는 IN_PROGRESS 상태 필터를 서비스로 전달한다`() {
        `when`(mypageService.getParticipations(1L, MypageParticipationStatusFilter.IN_PROGRESS)).thenReturn(emptyList())

        val result = controller.getUserParticipations(principal(), MypageParticipationStatusFilter.IN_PROGRESS)

        assertEquals(emptyList<Any>(), result.body?.data)
        verify(mypageService).getParticipations(1L, MypageParticipationStatusFilter.IN_PROGRESS)
    }

    @Test
    fun `users me participations는 PICKUP_WAITING 상태 필터를 서비스로 전달한다`() {
        `when`(mypageService.getParticipations(1L, MypageParticipationStatusFilter.PICKUP_WAITING)).thenReturn(emptyList())

        controller.getUserParticipations(principal(), MypageParticipationStatusFilter.PICKUP_WAITING)

        verify(mypageService).getParticipations(1L, MypageParticipationStatusFilter.PICKUP_WAITING)
    }

    @Test
    fun `users me participations는 PICKUP_COMPLETED 상태 필터를 서비스로 전달한다`() {
        `when`(mypageService.getParticipations(1L, MypageParticipationStatusFilter.PICKUP_COMPLETED)).thenReturn(emptyList())

        controller.getUserParticipations(principal(), MypageParticipationStatusFilter.PICKUP_COMPLETED)

        verify(mypageService).getParticipations(1L, MypageParticipationStatusFilter.PICKUP_COMPLETED)
    }

    @Test
    fun `users me participations는 CANCELLED_OR_REFUNDED 상태 필터를 서비스로 전달한다`() {
        `when`(mypageService.getParticipations(1L, MypageParticipationStatusFilter.CANCELLED_OR_REFUNDED)).thenReturn(emptyList())

        controller.getUserParticipations(principal(), MypageParticipationStatusFilter.CANCELLED_OR_REFUNDED)

        verify(mypageService).getParticipations(1L, MypageParticipationStatusFilter.CANCELLED_OR_REFUNDED)
    }

    @Test
    fun `users me tabs counts는 summary를 반환한다`() {
        val summary = MypageSummaryResponse(
            inProgressCount = 1,
            pickupWaitingCount = 2,
            pickupCompletedCount = 3,
            cancelledOrRefundedCount = 4,
            requestCount = 5
        )
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
