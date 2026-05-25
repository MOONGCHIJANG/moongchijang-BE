package com.moongchijang.domain.groupbuy.presentation

import com.moongchijang.domain.groupbuy.application.AdminGroupBuyRequestActionService
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestActionResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestApproveRequest
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestRejectRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AdminGroupBuyRequestActionControllerTest {

    private val service: AdminGroupBuyRequestActionService = mock(AdminGroupBuyRequestActionService::class.java)
    private val controller = AdminGroupBuyRequestActionController(service)

    @Test
    fun `공구 요청 승인 시 201과 생성된 공구 id를 반환한다`() {
        val request = approveRequest()
        val response = AdminGroupBuyRequestActionResponse(
            requestId = 10L,
            status = GroupBuyRequestStatus.OPENED,
            groupBuyId = 30L
        )
        `when`(service.approve(10L, request)).thenReturn(response)

        val result = controller.approve(10L, request)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(response, result.body?.data)
        verify(service).approve(10L, request)
    }

    @Test
    fun `공구 요청 반려 시 REJECTED 상태를 반환한다`() {
        val request = AdminGroupBuyRequestRejectRequest("재고 확보 불가")
        val response = AdminGroupBuyRequestActionResponse(
            requestId = 11L,
            status = GroupBuyRequestStatus.REJECTED,
            groupBuyId = null
        )
        `when`(service.reject(11L, request)).thenReturn(response)

        val result = controller.reject(11L, request)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body?.data)
        verify(service).reject(11L, request)
    }

    private fun approveRequest(): AdminGroupBuyRequestApproveRequest =
        AdminGroupBuyRequestApproveRequest(
            storeName = "뭉치장 베이커리",
            storeAddress = "서울 성동구 성수이로 1",
            storePhoneNumber = "01012345678",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN,
            productName = "두쫀쿠 세트",
            productDescription = "소금빵 포함",
            originalPrice = 12000,
            price = 9900,
            targetQuantity = 20,
            maxQuantity = 50,
            perUserLimit = 2,
            imageUrls = listOf("https://cdn.example.com/1.jpg"),
            recruitmentStartAt = LocalDateTime.now().minusHours(1),
            deadline = LocalDateTime.now().plusDays(3),
            pickupDate = LocalDate.now().plusDays(5),
            pickupTimeStart = LocalTime.of(12, 0),
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "서울 성동구 성수이로 1",
            pickupContact = "01012345678"
        )
}
