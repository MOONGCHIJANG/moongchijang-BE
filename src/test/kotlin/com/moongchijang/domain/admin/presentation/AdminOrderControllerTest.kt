package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminOrderService
import com.moongchijang.domain.admin.application.dto.AdminOrderPageResponse
import com.moongchijang.domain.admin.application.dto.AdminOrderStatusFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class AdminOrderControllerTest {

    private val adminOrderService: AdminOrderService = mock(AdminOrderService::class.java)
    private val controller = AdminOrderController(adminOrderService)

    @Test
    fun `운영자 발주 목록 조회는 상태 필터와 페이징을 서비스로 전달한다`() {
        val pageable = PageRequest.of(0, 20)
        val response = AdminOrderPageResponse(
            content = emptyList(),
            totalElements = 0,
            totalPages = 0,
            number = 0,
            size = 20
        )
        `when`(adminOrderService.getOrders(AdminOrderStatusFilter.PENDING, pageable)).thenReturn(response)

        val result = controller.getOrders(AdminOrderStatusFilter.PENDING, pageable)

        assertEquals(response, result.body?.data)
        verify(adminOrderService).getOrders(AdminOrderStatusFilter.PENDING, pageable)
    }
}
