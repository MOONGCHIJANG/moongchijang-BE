package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyCloseRequestReviewStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.owner.application.AdminOwnerGroupBuyCloseRequestService
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyCloseRequestActionResponse
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyCloseRequestRejectRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus

class AdminOwnerGroupBuyCloseRequestControllerTest {

    private val service: AdminOwnerGroupBuyCloseRequestService = mock(AdminOwnerGroupBuyCloseRequestService::class.java)
    private val controller = AdminOwnerGroupBuyCloseRequestController(service)

    @Test
    fun `사장님 공구 마감 요청 승인 시 200과 CLOSED 상태를 반환한다`() {
        val response = AdminOwnerGroupBuyCloseRequestActionResponse(
            groupBuyId = 21L,
            reviewStatus = GroupBuyCloseRequestReviewStatus.APPROVED,
            groupBuyStatus = GroupBuyStatus.CLOSED
        )
        `when`(service.approve(21L)).thenReturn(response)

        val result = controller.approve(21L)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body?.data)
        verify(service).approve(21L)
    }

    @Test
    fun `사장님 공구 마감 요청 반려 시 REJECTED 상태를 반환한다`() {
        val request = AdminOwnerGroupBuyCloseRequestRejectRequest("사유가 불충분합니다")
        val response = AdminOwnerGroupBuyCloseRequestActionResponse(
            groupBuyId = 22L,
            reviewStatus = GroupBuyCloseRequestReviewStatus.REJECTED,
            groupBuyStatus = GroupBuyStatus.ACHIEVED
        )
        `when`(service.reject(22L, request)).thenReturn(response)

        val result = controller.reject(22L, request)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body?.data)
        verify(service).reject(22L, request)
    }
}
