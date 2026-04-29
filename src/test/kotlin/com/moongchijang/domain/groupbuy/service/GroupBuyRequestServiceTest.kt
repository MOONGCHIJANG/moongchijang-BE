package com.moongchijang.domain.groupbuy.service

import com.moongchijang.application.groupbuy.GroupBuyRequestService
import com.moongchijang.application.groupbuy.dto.GroupBuyRequestCreateRequest
import com.moongchijang.domain.groupbuy.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class GroupBuyRequestServiceTest {

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    @Mock
    private lateinit var groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository

    @InjectMocks
    private lateinit var service: GroupBuyRequestService

    @Test
    fun `유효한 입력으로 요청 시 requestId 반환`() {
        val userId = 1L
        val request = createRequest(desiredPickupDate = LocalDate.now().plusDays(3))
        val saved = GroupBuyRequest(
            userId = userId,
            storeName = request.storeName,
            storeAddress = request.storeAddress,
            productName = request.productName,
            desiredQuantity = request.desiredQuantity,
            desiredPickupDate = request.desiredPickupDate,
            additionalNote = request.additionalNote
        ).apply { id = 42L }

        `when`(groupBuyRequestRepository.save(any())).thenReturn(saved)
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenReturn(
            GroupBuyRequestStatusHistory(groupBuyRequestId = 42L, status = GroupBuyRequestStatus.SUBMITTED)
        )

        val result = service.create(userId, request)

        assertEquals(42L, result.requestId)
        verify(groupBuyRequestRepository).save(any())
        verify(groupBuyRequestStatusHistoryRepository).save(any())
    }

    @Test
    fun `오늘 날짜로 요청 시 GROUPBUY_REQUEST_INVALID_DATE 예외`() {
        val request = createRequest(desiredPickupDate = LocalDate.now())

        val ex = assertThrows<CustomException> { service.create(1L, request) }
        assertEquals(ErrorCode.GROUPBUY_REQUEST_INVALID_DATE, ex.errorCode)
    }

    @Test
    fun `과거 날짜로 요청 시 GROUPBUY_REQUEST_INVALID_DATE 예외`() {
        val request = createRequest(desiredPickupDate = LocalDate.now().minusDays(1))

        val ex = assertThrows<CustomException> { service.create(1L, request) }
        assertEquals(ErrorCode.GROUPBUY_REQUEST_INVALID_DATE, ex.errorCode)
    }

    @Test
    fun `내 요청 목록 조회 시 본인 요청만 반환`() {
        val userId = 1L
        val requests = listOf(
            GroupBuyRequest(userId = userId, storeName = "성심당", productName = "튀김소보로",
                desiredQuantity = 2, desiredPickupDate = LocalDate.now().plusDays(5)).apply { id = 1L }
        )
        `when`(groupBuyRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(requests)
        `when`(groupBuyRequestStatusHistoryRepository.findByGroupBuyRequestIdInOrderByChangedAtAsc(listOf(1L)))
            .thenReturn(listOf(
                GroupBuyRequestStatusHistory(groupBuyRequestId = 1L, status = GroupBuyRequestStatus.SUBMITTED,
                    changedAt = LocalDateTime.now())
            ))

        val result = service.getMyRequests(userId)

        assertEquals(1, result.size)
        assertEquals("성심당", result[0].storeName)
        assertEquals(GroupBuyRequestStatus.SUBMITTED.name, result[0].status)
        assertEquals(1, result[0].statusHistory.size)
    }

    @Test
    fun `요청 목록이 없으면 빈 리스트 반환`() {
        `when`(groupBuyRequestRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(emptyList())

        val result = service.getMyRequests(1L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `상세 조회 시 statusHistory 포함 반환`() {
        val userId = 1L
        val requestId = 10L
        val groupBuyRequest = GroupBuyRequest(userId = userId, storeName = "뚜레쥬르", productName = "크림빵",
            desiredQuantity = 1, desiredPickupDate = LocalDate.now().plusDays(7)).apply { id = requestId }
        val history = listOf(
            GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.SUBMITTED,
                changedAt = LocalDateTime.now().minusDays(2)),
            GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.REVIEWING,
                changedAt = LocalDateTime.now().minusDays(1))
        )

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))
        `when`(groupBuyRequestStatusHistoryRepository.findByGroupBuyRequestIdOrderByChangedAtAsc(requestId))
            .thenReturn(history)

        val result = service.getDetail(userId, requestId)

        assertEquals(requestId, result.requestId)
        assertEquals(2, result.statusHistory.size)
        assertEquals(GroupBuyRequestStatus.SUBMITTED.name, result.statusHistory[0].status)
        assertEquals(GroupBuyRequestStatus.REVIEWING.name, result.statusHistory[1].status)
    }

    @Test
    fun `존재하지 않는 요청 조회 시 GROUPBUY_REQUEST_NOT_FOUND 예외`() {
        `when`(groupBuyRequestRepository.findById(999L)).thenReturn(Optional.empty())

        val ex = assertThrows<CustomException> { service.getDetail(1L, 999L) }
        assertEquals(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `다른 사용자의 요청 조회 시 GROUPBUY_REQUEST_FORBIDDEN 예외`() {
        val requestId = 10L
        val groupBuyRequest = GroupBuyRequest(userId = 2L, storeName = "파리바게뜨", productName = "단팥빵",
            desiredQuantity = 1, desiredPickupDate = LocalDate.now().plusDays(5)).apply { id = requestId }

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))

        val ex = assertThrows<CustomException> { service.getDetail(1L, requestId) }
        assertEquals(ErrorCode.GROUPBUY_REQUEST_FORBIDDEN, ex.errorCode)
    }

    private fun createRequest(
        storeName: String = "성심당",
        productName: String = "튀김소보로",
        desiredQuantity: Int = 2,
        desiredPickupDate: LocalDate = LocalDate.now().plusDays(3)
    ) = GroupBuyRequestCreateRequest(
        storeName = storeName,
        productName = productName,
        desiredQuantity = desiredQuantity,
        desiredPickupDate = desiredPickupDate
    )
}
