package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.OwnerGroupBuyRequestService
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestDetailResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestPageResponse
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.security.principal.CustomUserPrincipal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class OwnerGroupBuyRequestControllerTest {

    private val ownerGroupBuyRequestService: OwnerGroupBuyRequestService = mock(OwnerGroupBuyRequestService::class.java)
    private val controller = OwnerGroupBuyRequestController(ownerGroupBuyRequestService)

    @Test
    fun `사장님 공구 개설 요청 목록을 조회한다`() {
        val principal = principal()
        val pageable = PageRequest.of(0, 20)
        val response = OwnerGroupBuyRequestPageResponse(
            content = listOf(
                OwnerGroupBuyRequestListItemResponse(
                    requestId = 101L,
                    productName = "두쫀쿠 세트",
                    storeName = "뭉치장 베이커리",
                    originalPrice = 12000,
                    price = 9900,
                    targetQuantity = 20,
                    pickupDate = LocalDate.of(2026, 6, 3),
                    requestedAt = LocalDateTime.of(2026, 5, 25, 12, 0),
                    status = OwnerGroupBuyRequestStatus.PENDING,
                    rejectionReason = null,
                    approvedGroupBuyId = null
                )
            ),
            totalElements = 1,
            totalPages = 1,
            number = 0,
            size = 20
        )
        `when`(ownerGroupBuyRequestService.getMyRequests(1L, pageable)).thenReturn(response)

        val result = controller.getMyRequests(principal, pageable)

        assertEquals(response, result.body?.data)
        verify(ownerGroupBuyRequestService).getMyRequests(1L, pageable)
    }

    @Test
    fun `사장님 공구 개설 요청 상세를 조회한다`() {
        val principal = principal()
        val deadline = LocalDateTime.of(2026, 6, 2, 23, 59)
        val response = OwnerGroupBuyRequestDetailResponse(
            requestId = 101L,
            storeId = 1L,
            storeName = "뭉치장 베이커리",
            productName = "두쫀쿠 세트",
            productDescription = "소금빵 포함",
            originalPrice = 12000,
            price = 9900,
            targetQuantity = 20,
            maxQuantity = 50,
            perUserLimit = 2,
            thumbnailUrl = "https://cdn.example.com/1.jpg",
            imageUrls = listOf("https://cdn.example.com/1.jpg"),
            deadline = deadline,
            pickupDate = LocalDate.of(2026, 6, 3),
            pickupTimeStart = LocalTime.of(12, 0),
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "서울 성동구 성수이로 1",
            pickupContact = "01012345678",
            status = OwnerGroupBuyRequestStatus.PENDING,
            rejectionReason = null,
            approvedGroupBuyId = null,
            reviewedAt = null,
            requestedAt = LocalDateTime.of(2026, 5, 25, 12, 0)
        )
        `when`(ownerGroupBuyRequestService.getDetail(1L, 101L)).thenReturn(response)

        val result = controller.getDetail(principal, 101L)

        assertEquals(response, result.body?.data)
        verify(ownerGroupBuyRequestService).getDetail(1L, 101L)
    }

    @Test
    fun `사장님 공구 개설 요청 제출 시 201과 요청 ID를 반환한다`() {
        val principal = principal()
        val deadline = LocalDateTime.now().plusDays(8)
        val request = OwnerGroupBuyRequestCreateRequest(
            storeId = 1L,
            productName = "두쫀쿠 세트",
            productDescription = "소금빵 포함",
            deadline = deadline,
            originalPrice = 12000,
            price = 9900,
            targetQuantity = 20,
            maxQuantity = 50,
            perUserLimit = 2,
            imageUrls = listOf("https://cdn.example.com/1.jpg"),
            pickupDate = deadline.toLocalDate().plusDays(1),
            pickupTimeStart = LocalTime.of(12, 0),
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "서울 성동구 성수이로 1",
            pickupContact = "01012345678"
        )
        val response = OwnerGroupBuyRequestCreateResponse(
            requestId = 101L,
            status = OwnerGroupBuyRequestStatus.PENDING
        )
        `when`(ownerGroupBuyRequestService.create(1L, request)).thenReturn(response)

        val result = controller.create(principal, request)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(response, result.body?.data)
        verify(ownerGroupBuyRequestService).create(1L, request)
    }

    private fun principal() = CustomUserPrincipal(
        id = 1L,
        email = "seller@example.com",
        role = UserRole.SELLER
    )
}
