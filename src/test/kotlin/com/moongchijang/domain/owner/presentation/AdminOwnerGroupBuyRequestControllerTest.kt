package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.AdminOwnerGroupBuyRequestService
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyRequestActionResponse
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyRequestRejectRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus

class AdminOwnerGroupBuyRequestControllerTest {

    private val service: AdminOwnerGroupBuyRequestService = mock(AdminOwnerGroupBuyRequestService::class.java)
    private val controller = AdminOwnerGroupBuyRequestController(service)

    @Test
    fun `사장님 공구 요청 승인 시 201과 생성된 공구 id를 반환한다`() {
        val response = AdminOwnerGroupBuyRequestActionResponse(
            requestId = 10L,
            status = OwnerGroupBuyRequestStatus.APPROVED,
            groupBuyId = 30L
        )
        `when`(service.approve(10L)).thenReturn(response)

        val result = controller.approve(10L)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(response, result.body?.data)
        verify(service).approve(10L)
    }

    @Test
    fun `사장님 공구 요청 반려 시 REJECTED 상태를 반환한다`() {
        val request = AdminOwnerGroupBuyRequestRejectRequest("재고 확보 불가")
        val response = AdminOwnerGroupBuyRequestActionResponse(
            requestId = 11L,
            status = OwnerGroupBuyRequestStatus.REJECTED,
            groupBuyId = null
        )
        `when`(service.reject(11L, request)).thenReturn(response)

        val result = controller.reject(11L, request)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body?.data)
        verify(service).reject(11L, request)
    }
}
