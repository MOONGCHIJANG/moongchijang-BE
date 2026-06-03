package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.OwnerGroupBuyService
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyCloseReasonType
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyCloseRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyExtensionRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageDetailResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageFilterType
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageParticipantSummary
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyStatusResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuySummaryResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.security.principal.CustomUserPrincipal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime

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
            settlementExpectedAmount = 128000L,
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

    @Test
    fun `사장님 공구 관리 목록을 조회한다`() {
        val principal = CustomUserPrincipal(id = 1L, email = "seller@example.com", role = UserRole.SELLER)
        val response = listOf(
            OwnerGroupBuyManageListItemResponse(
                groupBuyId = 101L,
                productName = "두쫀쿠 세트",
                price = 9900,
                pickupDate = LocalDate.of(2026, 6, 1),
                deadlineDday = 3,
                achievementRate = 60,
                currentQuantity = 12,
                targetQuantity = 20,
                status = OwnerGroupBuyManageFilterType.IN_PROGRESS
            )
        )
        `when`(ownerGroupBuyService.getManageGroupBuys(1L, OwnerGroupBuyManageFilterType.ALL)).thenReturn(response)

        val result = controller.getManageGroupBuys(principal, OwnerGroupBuyManageFilterType.ALL)

        assertEquals(response, result.body?.data)
        verify(ownerGroupBuyService).getManageGroupBuys(1L, OwnerGroupBuyManageFilterType.ALL)
    }

    @Test
    fun `사장님 공구 관리 승인대기 목록은 requestId를 포함한다`() {
        val principal = CustomUserPrincipal(id = 1L, email = "seller@example.com", role = UserRole.SELLER)
        val response = listOf(
            OwnerGroupBuyManageListItemResponse(
                requestId = 55L,
                productName = "승인대기 공구",
                price = 9900,
                pickupDate = LocalDate.of(2026, 6, 12),
                status = OwnerGroupBuyManageFilterType.PENDING_APPROVAL
            )
        )
        `when`(ownerGroupBuyService.getManageGroupBuys(1L, OwnerGroupBuyManageFilterType.PENDING_APPROVAL)).thenReturn(response)

        val result = controller.getManageGroupBuys(principal, OwnerGroupBuyManageFilterType.PENDING_APPROVAL)

        assertEquals(55L, result.body?.data?.first()?.requestId)
        assertNull(result.body?.data?.first()?.groupBuyId)
        verify(ownerGroupBuyService).getManageGroupBuys(1L, OwnerGroupBuyManageFilterType.PENDING_APPROVAL)
    }

    @Test
    fun `사장님 모집중 공구 상세를 조회한다`() {
        val principal = CustomUserPrincipal(id = 1L, email = "seller@example.com", role = UserRole.SELLER)
        val response = OwnerGroupBuyManageDetailResponse(
            groupBuyId = 101L,
            status = OwnerGroupBuyManageFilterType.IN_PROGRESS,
            participantSummary = OwnerGroupBuyManageParticipantSummary(totalCount = 20, completedCount = 8, waitingCount = 12),
            participants = emptyList()
        )
        `when`(ownerGroupBuyService.getInProgressGroupBuyDetail(1L, 101L)).thenReturn(response)

        val result = controller.getInProgressGroupBuyDetail(principal, 101L)

        assertEquals(response, result.body?.data)
        verify(ownerGroupBuyService).getInProgressGroupBuyDetail(1L, 101L)
    }

    @Test
    fun `사장님 달성 공구 상세를 조회한다`() {
        val principal = CustomUserPrincipal(id = 1L, email = "seller@example.com", role = UserRole.SELLER)
        val response = OwnerGroupBuyManageDetailResponse(
            groupBuyId = 102L,
            status = OwnerGroupBuyManageFilterType.ACHIEVED,
            participantSummary = OwnerGroupBuyManageParticipantSummary(totalCount = 20, completedCount = 8, waitingCount = 12),
            participants = emptyList()
        )
        `when`(ownerGroupBuyService.getAchievedGroupBuyDetail(1L, 102L)).thenReturn(response)

        val result = controller.getAchievedGroupBuyDetail(principal, 102L)

        assertEquals(response, result.body?.data)
        verify(ownerGroupBuyService).getAchievedGroupBuyDetail(1L, 102L)
    }

    @Test
    fun `사장님 공구 기간 연장 요청을 한다`() {
        val principal = CustomUserPrincipal(id = 1L, email = "seller@example.com", role = UserRole.SELLER)
        val request = OwnerGroupBuyExtensionRequest(
            extendedDeadline = LocalDateTime.of(2026, 6, 10, 23, 59)
        )

        val result = controller.requestGroupBuyExtension(principal, 101L, request)

        assertEquals(true, result.body?.success)
        assertNull(result.body?.data)
        verify(ownerGroupBuyService).requestGroupBuyExtension(1L, 101L, request)
    }

    @Test
    fun `사장님 공구 마감 요청을 한다`() {
        val principal = CustomUserPrincipal(id = 1L, email = "seller@example.com", role = UserRole.SELLER)
        val request = OwnerGroupBuyCloseRequest(
            reason = OwnerGroupBuyCloseReasonType.OTHER,
            reasonDetail = "재고 수급 이슈로 조기 마감합니다."
        )

        val result = controller.requestGroupBuyClose(principal, 101L, request)

        assertEquals(true, result.body?.success)
        assertNull(result.body?.data)
        verify(ownerGroupBuyService).requestGroupBuyClose(1L, 101L, request)
    }
}
