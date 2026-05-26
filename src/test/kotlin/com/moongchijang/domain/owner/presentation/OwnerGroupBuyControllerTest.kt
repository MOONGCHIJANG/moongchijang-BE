package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.OwnerGroupBuyService
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyStatusResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuySummaryResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.security.principal.CustomUserPrincipal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate

class OwnerGroupBuyControllerTest {

    private val ownerGroupBuyService: OwnerGroupBuyService = mock(OwnerGroupBuyService::class.java)
    private val controller = OwnerGroupBuyController(ownerGroupBuyService)

    @Test
    fun `사장님 공구 요약을 조회한다`() {
        val principal = CustomUserPrincipal(
            id = 1L,
            email = "seller@example.com",
            role = UserRole.SELLER
        )
        val response = OwnerGroupBuySummaryResponse(
            ongoingCount = 3,
            achievedCount = 2,
            todayPickupUserCount = 14,
            settlementExpectedAmount = 128000,
            isEmpty = false
        )
        `when`(ownerGroupBuyService.getMyGroupBuySummary(1L)).thenReturn(response)

        val result = controller.getMyGroupBuySummary(principal)

        assertEquals(response, result.body?.data)
        verify(ownerGroupBuyService).getMyGroupBuySummary(1L)
    }

    @Test
    fun `사장님 진행 중인 공구 목록을 조회한다`() {
        val principal = CustomUserPrincipal(
            id = 1L,
            email = "seller@example.com",
            role = UserRole.SELLER
        )
        val response = listOf(
            OwnerGroupBuyListItemResponse(
                groupBuyId = 10L,
                productName = "두쫀쿠 1개",
                targetQuantity = 20,
                currentQuantity = 12,
                achievementRate = 60,
                price = 9900,
                deadline = LocalDate.of(2026, 6, 1),
                status = OwnerGroupBuyStatusResponse.IN_PROGRESS
            )
        )
        `when`(ownerGroupBuyService.getMyGroupBuys(1L)).thenReturn(response)

        val result = controller.getMyGroupBuys(principal)

        assertEquals(response, result.body?.data)
        verify(ownerGroupBuyService).getMyGroupBuys(1L)
    }
}
