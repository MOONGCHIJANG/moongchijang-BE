package com.moongchijang.domain.admin.presentation

import com.moongchijang.domain.admin.application.AdminOrderService
import com.moongchijang.domain.admin.application.dto.AdminOrderDetailResponse
import com.moongchijang.domain.admin.application.dto.AdminOrderPageResponse
import com.moongchijang.domain.admin.application.dto.AdminOrderStatusFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

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

    @Test
    fun `운영자 발주 상세 조회는 주문 ID를 서비스로 전달한다`() {
        val response = detailResponse(orderId = 30L)
        `when`(adminOrderService.getOrderDetail(30L)).thenReturn(response)

        val result = controller.getOrderDetail(30L)

        assertEquals(response, result.body?.data)
        verify(adminOrderService).getOrderDetail(30L)
    }

    @Test
    fun `운영자 발주 확정 처리는 주문 ID를 서비스로 전달한다`() {
        val response = detailResponse(orderId = 31L, orderStatus = GroupBuyOrderStatus.CONFIRMED)
        `when`(adminOrderService.confirmOrder(31L)).thenReturn(response)

        val result = controller.confirmOrder(31L)

        assertEquals(response, result.body?.data)
        verify(adminOrderService).confirmOrder(31L)
    }

    private fun detailResponse(
        orderId: Long,
        orderStatus: GroupBuyOrderStatus = GroupBuyOrderStatus.PENDING,
    ): AdminOrderDetailResponse =
        AdminOrderDetailResponse(
            orderId = orderId,
            groupBuyId = orderId,
            productName = "두쫀쿠 1개",
            productDescription = "설명",
            storeName = "뭉치장 베이커리",
            storeAddress = "서울 성동구",
            storePhoneNumber = null,
            achievedAt = LocalDateTime.of(2026, 5, 27, 10, 0),
            finalQuantity = 20,
            targetQuantity = 20,
            pendingRefundCount = 0,
            pickupDate = LocalDate.of(2026, 6, 1),
            pickupTimeStart = "14:00",
            pickupTimeEnd = "18:00",
            pickupLocation = "서울 성동구 성수동",
            pickupContact = null,
            elapsedHours = 3,
            progressRate = 100,
            orderStatus = orderStatus,
            ownerContactedAt = null,
            orderConfirmedAt = null,
            orderCancelledAt = null,
            actionable = orderStatus == GroupBuyOrderStatus.PENDING
        )
}
