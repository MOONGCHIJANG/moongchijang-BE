package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyRequestService
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestCreateRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
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
        val request = createRequest(
            desiredPickupDate = LocalDate.now().plusDays(3),
            contactPhone = "010-1234-5678",
            contactInstagram = "moongchi.bread"
        )
        val saved = GroupBuyRequest(
            userId = userId,
            storeName = request.storeName,
            storeAddress = request.storeAddress,
            productName = request.productName,
            desiredQuantity = request.desiredQuantity,
            desiredPickupDate = request.desiredPickupDate,
            additionalNote = request.additionalNote,
            contactPhone = request.contactPhone,
            contactInstagram = request.contactInstagram
        ).apply { id = 42L }

        `when`(groupBuyRequestRepository.save(any())).thenReturn(saved)
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenReturn(
            GroupBuyRequestStatusHistory(groupBuyRequestId = 42L, status = GroupBuyRequestStatus.IN_REVIEW)
        )

        val result = service.create(userId, request)

        assertEquals(42L, result.requestId)
        val captor = argumentCaptor<GroupBuyRequest>()
        verify(groupBuyRequestRepository).save(captor.capture())
        assertEquals("010-1234-5678", captor.value.contactPhone)
        assertEquals("moongchi.bread", captor.value.contactInstagram)
        assertNull(captor.value.placeId)
        assertNull(captor.value.latitude)
        assertNull(captor.value.longitude)
        verify(groupBuyRequestStatusHistoryRepository).save(any())
    }

    @Test
    fun `네이버 장소 선택 정보가 있으면 요청에 함께 저장한다`() {
        val userId = 1L
        val request = createRequest(
            storeAddress = "서울 성동구 성수동1가 1",
            placeId = "naver-place-1",
            roadAddress = "서울 성동구 성수이로 1",
            lotAddress = "서울 성동구 성수동1가 1",
            latitude = 37.5,
            longitude = 127.0
        )
        val saved = GroupBuyRequest(
            userId = userId,
            storeName = request.storeName,
            storeAddress = request.roadAddress,
            placeId = request.placeId,
            roadAddress = request.roadAddress,
            lotAddress = request.lotAddress,
            latitude = request.latitude,
            longitude = request.longitude,
            productName = request.productName,
            desiredQuantity = request.desiredQuantity,
            desiredPickupDate = request.desiredPickupDate,
            additionalNote = request.additionalNote,
            contactPhone = request.contactPhone,
            contactInstagram = request.contactInstagram
        ).apply { id = 43L }

        `when`(groupBuyRequestRepository.save(any())).thenReturn(saved)
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenReturn(
            GroupBuyRequestStatusHistory(groupBuyRequestId = 43L, status = GroupBuyRequestStatus.IN_REVIEW)
        )

        val result = service.create(userId, request)

        assertEquals(43L, result.requestId)
        val captor = argumentCaptor<GroupBuyRequest>()
        verify(groupBuyRequestRepository).save(captor.capture())
        assertEquals("서울 성동구 성수이로 1", captor.value.storeAddress)
        assertEquals("naver-place-1", captor.value.placeId)
        assertEquals("서울 성동구 성수이로 1", captor.value.roadAddress)
        assertEquals("서울 성동구 성수동1가 1", captor.value.lotAddress)
        assertEquals(37.5, captor.value.latitude)
        assertEquals(127.0, captor.value.longitude)
    }

    @Test
    fun `도로명 주소가 빈 값이면 기존 매장 주소를 저장한다`() {
        val userId = 1L
        val request = createRequest(
            storeAddress = "서울 성동구 성수동1가 1",
            placeId = " ",
            roadAddress = " ",
            lotAddress = " ",
            latitude = 37.5,
            longitude = 127.0
        )
        val saved = GroupBuyRequest(
            userId = userId,
            storeName = request.storeName,
            storeAddress = request.storeAddress,
            productName = request.productName,
            desiredQuantity = request.desiredQuantity,
            desiredPickupDate = request.desiredPickupDate
        ).apply { id = 44L }

        `when`(groupBuyRequestRepository.save(any())).thenReturn(saved)
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenReturn(
            GroupBuyRequestStatusHistory(groupBuyRequestId = 44L, status = GroupBuyRequestStatus.IN_REVIEW)
        )

        service.create(userId, request)

        val captor = argumentCaptor<GroupBuyRequest>()
        verify(groupBuyRequestRepository).save(captor.capture())
        assertEquals("서울 성동구 성수동1가 1", captor.value.storeAddress)
        assertNull(captor.value.placeId)
        assertNull(captor.value.roadAddress)
        assertNull(captor.value.lotAddress)
        assertNull(captor.value.latitude)
        assertNull(captor.value.longitude)
    }

    @Test
    fun `도로명 주소가 빈 값이면 지번 주소를 매장 주소로 저장한다`() {
        val userId = 1L
        val request = createRequest(
            storeAddress = "서울 성동구 기존 주소",
            placeId = "naver-place-2",
            roadAddress = " ",
            lotAddress = "서울 성동구 성수동1가 1",
            latitude = 37.5,
            longitude = 127.0
        )
        val saved = GroupBuyRequest(
            userId = userId,
            storeName = request.storeName,
            storeAddress = request.lotAddress,
            productName = request.productName,
            desiredQuantity = request.desiredQuantity,
            desiredPickupDate = request.desiredPickupDate
        ).apply { id = 45L }

        `when`(groupBuyRequestRepository.save(any())).thenReturn(saved)
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenReturn(
            GroupBuyRequestStatusHistory(groupBuyRequestId = 45L, status = GroupBuyRequestStatus.IN_REVIEW)
        )

        service.create(userId, request)

        val captor = argumentCaptor<GroupBuyRequest>()
        verify(groupBuyRequestRepository).save(captor.capture())
        assertEquals("서울 성동구 성수동1가 1", captor.value.storeAddress)
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
                GroupBuyRequestStatusHistory(groupBuyRequestId = 1L, status = GroupBuyRequestStatus.IN_REVIEW,
                    changedAt = LocalDateTime.now())
            ))

        val result = service.getMyRequests(userId)

        assertEquals(1, result.size)
        assertEquals("성심당", result[0].storeName)
        assertNull(result[0].contactPhone)
        assertNull(result[0].contactInstagram)
        assertEquals(GroupBuyRequestStatus.IN_REVIEW.name, result[0].status)
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
            desiredQuantity = 1, desiredPickupDate = LocalDate.now().plusDays(7),
            contactPhone = "010-9876-5432", contactInstagram = "bakery.pickup",
            placeId = "naver-place-2", roadAddress = "서울 강남구 도산대로 1",
            lotAddress = "서울 강남구 신사동 1", latitude = 37.1, longitude = 127.1).apply { id = requestId }
        val history = listOf(
            GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.IN_REVIEW,
                changedAt = LocalDateTime.now().minusDays(2)),
            GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.IN_CONTACT,
                changedAt = LocalDateTime.now().minusDays(1))
        )

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))
        `when`(groupBuyRequestStatusHistoryRepository.findByGroupBuyRequestIdOrderByChangedAtAsc(requestId))
            .thenReturn(history)

        val result = service.getDetail(userId, requestId)

        assertEquals(requestId, result.requestId)
        assertEquals("010-9876-5432", result.contactPhone)
        assertEquals("bakery.pickup", result.contactInstagram)
        assertEquals("naver-place-2", result.placeId)
        assertEquals("서울 강남구 도산대로 1", result.roadAddress)
        assertEquals("서울 강남구 신사동 1", result.lotAddress)
        assertEquals(37.1, result.latitude)
        assertEquals(127.1, result.longitude)
        assertEquals(2, result.statusHistory.size)
        assertEquals(GroupBuyRequestStatus.IN_REVIEW.name, result.statusHistory[0].status)
        assertEquals(GroupBuyRequestStatus.IN_CONTACT.name, result.statusHistory[1].status)
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
        storeAddress: String? = null,
        placeId: String? = null,
        roadAddress: String? = null,
        lotAddress: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        productName: String = "튀김소보로",
        desiredQuantity: Int = 2,
        desiredPickupDate: LocalDate = LocalDate.now().plusDays(3),
        contactPhone: String? = null,
        contactInstagram: String? = null
    ) = GroupBuyRequestCreateRequest(
        storeName = storeName,
        storeAddress = storeAddress,
        placeId = placeId,
        roadAddress = roadAddress,
        lotAddress = lotAddress,
        latitude = latitude,
        longitude = longitude,
        productName = productName,
        desiredQuantity = desiredQuantity,
        desiredPickupDate = desiredPickupDate,
        contactPhone = contactPhone,
        contactInstagram = contactInstagram
    )

    private inline fun <reified T> argumentCaptor() = org.mockito.ArgumentCaptor.forClass(T::class.java)
}
