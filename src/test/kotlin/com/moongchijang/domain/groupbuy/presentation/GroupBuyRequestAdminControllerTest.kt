package com.moongchijang.domain.groupbuy.presentation

import com.moongchijang.domain.groupbuy.application.GroupBuyRequestService
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestDetailResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestPageResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestRequesterResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestStatusFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.user.domain.entity.AuthProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

class GroupBuyRequestAdminControllerTest {

    private val groupBuyRequestService: GroupBuyRequestService = mock(GroupBuyRequestService::class.java)
    private val controller = GroupBuyRequestAdminController(groupBuyRequestService)

    @Test
    fun `운영자 공구 요청 목록 조회는 상태 필터와 페이징을 서비스로 전달한다`() {
        val pageable = PageRequest.of(0, 20)
        val response = AdminGroupBuyRequestPageResponse(
            content = emptyList(),
            totalElements = 0,
            totalPages = 0,
            number = 0,
            size = 20
        )
        `when`(groupBuyRequestService.getAdminRequests(AdminGroupBuyRequestStatusFilter.IN_REVIEW, "성심당", pageable))
            .thenReturn(response)

        val result = controller.getRequests(AdminGroupBuyRequestStatusFilter.IN_REVIEW, "성심당", pageable)

        assertEquals(response, result.body?.data)
        verify(groupBuyRequestService).getAdminRequests(AdminGroupBuyRequestStatusFilter.IN_REVIEW, "성심당", pageable)
    }

    @Test
    fun `운영자 공구 요청 상세 조회는 요청 id를 서비스로 전달한다`() {
        val response = AdminGroupBuyRequestDetailResponse(
            requestId = 10L,
            requester = AdminGroupBuyRequestRequesterResponse(
                userId = 1L,
                nickname = "요청자",
                phoneNumber = "01012345678",
                email = "requester@example.com",
                provider = AuthProvider.KAKAO
            ),
            storeName = "성심당",
            storeAddress = "대전 중구",
            placeId = null,
            roadAddress = null,
            lotAddress = null,
            latitude = null,
            longitude = null,
            productName = "튀김소보로",
            desiredQuantity = 20,
            desiredPickupDate = LocalDate.of(2026, 6, 1),
            additionalNote = "오전 픽업 희망",
            status = GroupBuyRequestStatus.IN_REVIEW,
            rejectionReason = null,
            openedGroupBuyId = null,
            statusHistory = emptyList(),
            createdAt = LocalDateTime.of(2026, 5, 25, 12, 0)
        )
        `when`(groupBuyRequestService.getAdminDetail(10L)).thenReturn(response)

        val result = controller.getDetail(10L)

        assertEquals(response, result.body?.data)
        verify(groupBuyRequestService).getAdminDetail(10L)
    }
}
