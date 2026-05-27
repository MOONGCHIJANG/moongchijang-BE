package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyRequestService
import com.moongchijang.domain.groupbuy.application.GroupBuyOpenRequestService
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestStatusFilter
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestStatusUpdateRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.GroupBuyRequestFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class GroupBuyRequestServiceTest {

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    @Mock
    private lateinit var groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var groupBuyOpenRequestService: GroupBuyOpenRequestService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var clock: Clock

    @InjectMocks
    private lateinit var service: GroupBuyRequestService

    @BeforeEach
    fun setUpClock() {
        lenient().`when`(clock.instant()).thenReturn(Instant.parse("2026-05-27T04:00:00Z"))
        lenient().`when`(clock.zone).thenReturn(ZoneId.of("Asia/Seoul"))
    }

    @Test
    fun `유효한 입력으로 요청 시 requestId 반환`() {
        val userId = 1L
        val request = GroupBuyRequestFixture.createRequest(
            desiredPickupDate = LocalDate.now().plusDays(3)
        )
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
            GroupBuyRequestStatusHistory(groupBuyRequestId = 42L, status = GroupBuyRequestStatus.IN_REVIEW)
        )

        val result = service.create(userId, request)

        assertEquals(42L, result.requestId)
        val captor = argumentCaptor<GroupBuyRequest>()
        verify(groupBuyRequestRepository).save(captor.capture())
        assertNull(captor.value.placeId)
        assertNull(captor.value.latitude)
        assertNull(captor.value.longitude)
        verify(groupBuyRequestStatusHistoryRepository).save(any())
    }

    @Test
    fun `네이버 장소 선택 정보가 있으면 요청에 함께 저장한다`() {
        val userId = 1L
        val request = GroupBuyRequestFixture.createRequest(
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
            additionalNote = request.additionalNote
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
        val request = GroupBuyRequestFixture.createRequest(
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
        val request = GroupBuyRequestFixture.createRequest(
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
        val request = GroupBuyRequestFixture.createRequest(desiredPickupDate = LocalDate.now())

        val ex = assertThrows<CustomException> { service.create(1L, request) }
        assertEquals(ErrorCode.GROUPBUY_REQUEST_INVALID_DATE, ex.errorCode)
    }

    @Test
    fun `과거 날짜로 요청 시 GROUPBUY_REQUEST_INVALID_DATE 예외`() {
        val request = GroupBuyRequestFixture.createRequest(desiredPickupDate = LocalDate.now().minusDays(1))

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

    @Test
    fun `운영자는 전체 공구 요청 목록을 페이징 조회한다`() {
        val pageable = PageRequest.of(0, 20)
        val requester = UserFixture.createKakaoUser(id = 1L, nickname = "은서")
        val request = GroupBuyRequest(
            userId = 1L,
            storeName = "성심당",
            productName = "튀김소보로",
            desiredQuantity = 20,
            desiredPickupDate = LocalDate.now().plusDays(5)
        ).apply { id = 10L }

        `when`(groupBuyRequestRepository.searchAdminRequests(null, null, null, pageable))
            .thenReturn(PageImpl(listOf(request), pageable, 1))
        `when`(userRepository.findAllById(listOf(1L))).thenReturn(listOf(requester))

        val result = service.getAdminRequests(AdminGroupBuyRequestStatusFilter.ALL, null, pageable)

        assertEquals(1, result.content.size)
        assertEquals(1, result.totalElements)
        assertEquals(1, result.totalPages)
        assertEquals(0, result.number)
        assertEquals(20, result.size)
        assertEquals(10L, result.content[0].requestId)
        assertEquals("성심당", result.content[0].storeName)
        assertEquals("튀김소보로", result.content[0].productName)
        assertEquals(20, result.content[0].desiredQuantity)
        assertEquals(GroupBuyRequestStatus.IN_REVIEW, result.content[0].status)
        assertEquals(1L, result.content[0].requesterId)
        assertEquals("은서", result.content[0].requesterName)
        assertNull(result.content[0].price)
        assertTrue(result.content[0].actionable)
    }

    @Test
    fun `운영자는 상태별 공구 요청 목록을 페이징 조회한다`() {
        val pageable = PageRequest.of(1, 10)
        val request = GroupBuyRequest(
            userId = 2L,
            storeName = "파리바게뜨",
            productName = "단팥빵",
            desiredQuantity = 5,
            desiredPickupDate = LocalDate.now().plusDays(3),
            status = GroupBuyRequestStatus.REJECTED
        ).apply { id = 11L }

        `when`(groupBuyRequestRepository.searchAdminRequests(GroupBuyRequestStatus.REJECTED, null, null, pageable))
            .thenReturn(PageImpl(listOf(request), pageable, 11))
        `when`(userRepository.findAllById(listOf(2L))).thenReturn(emptyList())

        val result = service.getAdminRequests(AdminGroupBuyRequestStatusFilter.REJECTED, null, pageable)

        assertEquals(1, result.content.size)
        assertEquals(11, result.totalElements)
        assertEquals(2, result.totalPages)
        assertEquals(1, result.number)
        assertEquals(10, result.size)
        assertEquals(GroupBuyRequestStatus.REJECTED, result.content[0].status)
        assertEquals(2L, result.content[0].requesterId)
        assertNull(result.content[0].requesterName)
        assertFalse(result.content[0].actionable)
        verify(groupBuyRequestRepository).searchAdminRequests(GroupBuyRequestStatus.REJECTED, null, null, pageable)
    }

    @Test
    fun `운영자 공구 요청 목록이 비어있으면 사용자 조회를 생략한다`() {
        val pageable = PageRequest.of(0, 20)
        `when`(groupBuyRequestRepository.searchAdminRequests(null, null, null, pageable))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = service.getAdminRequests(AdminGroupBuyRequestStatusFilter.ALL, null, pageable)

        assertTrue(result.content.isEmpty())
        assertEquals(0, result.totalElements)
        verify(userRepository, never()).findAllById(anyList())
    }

    @Test
    fun `운영자 공구 요청 목록은 검색어와 승인 공구 가격을 반영한다`() {
        val pageable = PageRequest.of(0, 20)
        val request = GroupBuyRequest(
            userId = 1L,
            storeName = "성심당",
            productName = "튀김소보로",
            desiredQuantity = 20,
            desiredPickupDate = LocalDate.now().plusDays(5),
            status = GroupBuyRequestStatus.OPENED,
            openedGroupBuyId = 30L
        ).apply { id = 10L }
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 30L,
            status = GroupBuyStatus.IN_PROGRESS,
            price = 9_900
        ).apply {
            originalPrice = 12_000
        }

        `when`(groupBuyRequestRepository.searchAdminRequests(null, "10", 10L, pageable))
            .thenReturn(PageImpl(listOf(request), pageable, 1))
        `when`(userRepository.findAllById(listOf(1L))).thenReturn(emptyList())
        `when`(groupBuyRepository.findAllById(listOf(30L))).thenReturn(listOf(groupBuy))

        val result = service.getAdminRequests(AdminGroupBuyRequestStatusFilter.ALL, " 10 ", pageable)

        assertEquals(1, result.content.size)
        assertEquals(12_000, result.content[0].originalPrice)
        assertEquals(9_900, result.content[0].price)
        assertFalse(result.content[0].actionable)
        verify(groupBuyRequestRepository).searchAdminRequests(null, "10", 10L, pageable)
    }

    @Test
    fun `운영자는 공구 요청 상세를 조회한다`() {
        val requestId = 12L
        val requester = UserFixture.createKakaoUser(
            id = 3L,
            email = "requester@example.com",
            nickname = "요청자"
        ).apply { phoneNumber = "01012345678" }
        val request = GroupBuyRequest(
            userId = 3L,
            storeName = "뚜레쥬르",
            storeAddress = "서울 성동구",
            placeId = "place-1",
            roadAddress = "서울 성동구 도로명",
            lotAddress = "서울 성동구 지번",
            latitude = 37.1,
            longitude = 127.1,
            productName = "크림빵",
            desiredQuantity = 30,
            desiredPickupDate = LocalDate.now().plusDays(7),
            additionalNote = "오전 픽업 희망",
            status = GroupBuyRequestStatus.IN_CONTACT
        ).apply { id = requestId }
        val history = listOf(
            GroupBuyRequestStatusHistory(
                groupBuyRequestId = requestId,
                status = GroupBuyRequestStatus.IN_REVIEW,
                changedAt = LocalDateTime.now().minusDays(1)
            ),
            GroupBuyRequestStatusHistory(
                groupBuyRequestId = requestId,
                status = GroupBuyRequestStatus.IN_CONTACT,
                changedAt = LocalDateTime.now()
            )
        )

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        `when`(userRepository.findById(3L)).thenReturn(Optional.of(requester))
        `when`(groupBuyRequestStatusHistoryRepository.findByGroupBuyRequestIdOrderByChangedAtAsc(requestId))
            .thenReturn(history)

        val result = service.getAdminDetail(requestId)

        assertEquals(requestId, result.requestId)
        assertEquals(3L, result.requester.userId)
        assertEquals("요청자", result.requester.nickname)
        assertEquals("01012345678", result.requester.phoneNumber)
        assertEquals("requester@example.com", result.requester.email)
        assertEquals("뚜레쥬르", result.storeName)
        assertEquals("place-1", result.placeId)
        assertEquals("크림빵", result.productName)
        assertEquals(30, result.desiredQuantity)
        assertEquals("오전 픽업 희망", result.additionalNote)
        assertEquals(GroupBuyRequestStatus.IN_CONTACT, result.status)
        assertEquals(2, result.statusHistory.size)
        assertEquals(GroupBuyRequestStatus.IN_REVIEW, result.statusHistory[0].status)
        assertEquals(GroupBuyRequestStatus.IN_CONTACT, result.statusHistory[1].status)
    }

    @Test
    fun `IN_REVIEW 요청을 IN_CONTACT로 변경하면 상태와 히스토리를 저장한다`() {
        val requestId = 10L
        val groupBuyRequest = GroupBuyRequest(userId = 1L, storeName = "성심당", productName = "튀김소보로",
            desiredQuantity = 2, desiredPickupDate = LocalDate.now().plusDays(5)).apply { id = requestId }

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenReturn(
            GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.IN_CONTACT)
        )
        `when`(groupBuyRequestStatusHistoryRepository.findByGroupBuyRequestIdOrderByChangedAtAsc(requestId))
            .thenReturn(listOf(
                GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.IN_REVIEW),
                GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.IN_CONTACT)
            ))

        val result = service.updateStatus(
            requestId,
            GroupBuyRequestStatusUpdateRequest(targetStatus = GroupBuyRequestStatus.IN_CONTACT)
        )

        assertEquals(GroupBuyRequestStatus.IN_CONTACT, groupBuyRequest.status)
        assertEquals(GroupBuyRequestStatus.IN_CONTACT.name, result.status)
        assertEquals(2, result.statusHistory.size)
        val captor = argumentCaptor<GroupBuyRequestStatusHistory>()
        verify(groupBuyRequestStatusHistoryRepository).save(captor.capture())
        assertEquals(requestId, captor.value.groupBuyRequestId)
        assertEquals(GroupBuyRequestStatus.IN_CONTACT, captor.value.status)
        assertNotNull(captor.value.changedAt)
    }

    @Test
    fun `IN_CONTACT 요청을 REJECTED로 변경할 때 거절 사유를 저장한다`() {
        val requestId = 11L
        val groupBuyRequest = GroupBuyRequest(userId = 1L, storeName = "성심당", productName = "튀김소보로",
            desiredQuantity = 2, desiredPickupDate = LocalDate.now().plusDays(5),
            status = GroupBuyRequestStatus.IN_CONTACT).apply {
            id = requestId
            openedGroupBuyId = 99L
        }

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenReturn(
            GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.REJECTED)
        )
        `when`(groupBuyRequestStatusHistoryRepository.findByGroupBuyRequestIdOrderByChangedAtAsc(requestId))
            .thenReturn(listOf(
                GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.IN_REVIEW),
                GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.IN_CONTACT),
                GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.REJECTED)
            ))

        val result = service.updateStatus(
            requestId,
            GroupBuyRequestStatusUpdateRequest(
                targetStatus = GroupBuyRequestStatus.REJECTED,
                rejectionReason = "  공급 불가  "
            )
        )

        assertEquals(GroupBuyRequestStatus.REJECTED, groupBuyRequest.status)
        assertEquals("공급 불가", groupBuyRequest.rejectionReason)
        assertNull(groupBuyRequest.openedGroupBuyId)
        assertEquals("공급 불가", result.rejectionReason)
    }

    @Test
    fun `REJECTED 변경 시 거절 사유가 없으면 GROUPBUY_REQUEST_REJECTION_REASON_REQUIRED 예외`() {
        val requestId = 12L
        val groupBuyRequest = GroupBuyRequest(userId = 1L, storeName = "성심당", productName = "튀김소보로",
            desiredQuantity = 2, desiredPickupDate = LocalDate.now().plusDays(5),
            status = GroupBuyRequestStatus.IN_CONTACT).apply { id = requestId }

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))

        val ex = assertThrows<CustomException> {
            service.updateStatus(
                requestId,
                GroupBuyRequestStatusUpdateRequest(
                    targetStatus = GroupBuyRequestStatus.REJECTED,
                    rejectionReason = " "
                )
            )
        }

        assertEquals(ErrorCode.GROUPBUY_REQUEST_REJECTION_REASON_REQUIRED, ex.errorCode)
        verify(groupBuyRequestStatusHistoryRepository, never()).save(any())
    }

    @Test
    fun `IN_CONTACT 요청을 OPENED로 변경할 때 공구 id가 있으면 존재 검증 후 저장한다`() {
        val requestId = 13L
        val openedGroupBuyId = 100L
        val groupBuyRequest = GroupBuyRequest(userId = 1L, storeName = "성심당", productName = "튀김소보로",
            desiredQuantity = 2, desiredPickupDate = LocalDate.now().plusDays(5),
            status = GroupBuyRequestStatus.IN_CONTACT).apply {
            id = requestId
            rejectionReason = "이전 사유"
        }
        val openedGroupBuy = GroupBuyFixture.createGroupBuy(
            id = openedGroupBuyId,
            status = GroupBuyStatus.IN_PROGRESS,
            productName = "튀김소보로"
        )

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))
        `when`(groupBuyRepository.findWithStoreById(openedGroupBuyId)).thenReturn(Optional.of(openedGroupBuy))
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenReturn(
            GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.OPENED)
        )
        `when`(groupBuyRequestStatusHistoryRepository.findByGroupBuyRequestIdOrderByChangedAtAsc(requestId))
            .thenReturn(listOf(
                GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.IN_REVIEW),
                GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.IN_CONTACT),
                GroupBuyRequestStatusHistory(groupBuyRequestId = requestId, status = GroupBuyRequestStatus.OPENED)
            ))

        val result = service.updateStatus(
            requestId,
            GroupBuyRequestStatusUpdateRequest(
                targetStatus = GroupBuyRequestStatus.OPENED,
                openedGroupBuyId = openedGroupBuyId
            )
        )

        assertEquals(GroupBuyRequestStatus.OPENED, groupBuyRequest.status)
        assertNull(groupBuyRequest.rejectionReason)
        assertEquals(openedGroupBuyId, groupBuyRequest.openedGroupBuyId)
        assertEquals(openedGroupBuyId, result.openedGroupBuyId)
        verify(groupBuyRepository).findWithStoreById(openedGroupBuyId)
        verify(groupBuyOpenRequestService).notifyOpened(openedGroupBuy)
    }

    @Test
    fun `OPENED 변경 시 공구 id가 없으면 GROUPBUY_REQUEST_OPENED_GROUP_BUY_REQUIRED 예외`() {
        val requestId = 14L
        val groupBuyRequest = GroupBuyRequest(userId = 1L, storeName = "성심당", productName = "튀김소보로",
            desiredQuantity = 2, desiredPickupDate = LocalDate.now().plusDays(5),
            status = GroupBuyRequestStatus.IN_CONTACT).apply { id = requestId }

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))

        val ex = assertThrows<CustomException> {
            service.updateStatus(
                requestId,
                GroupBuyRequestStatusUpdateRequest(targetStatus = GroupBuyRequestStatus.OPENED)
            )
        }

        assertEquals(ErrorCode.GROUPBUY_REQUEST_OPENED_GROUP_BUY_REQUIRED, ex.errorCode)
        verifyNoInteractions(groupBuyRepository, groupBuyOpenRequestService)
        verify(groupBuyRequestStatusHistoryRepository, never()).save(any())
    }

    @Test
    fun `OPENED 변경 시 공구 id가 존재하지 않으면 GROUPBUY_NOT_FOUND 예외`() {
        val requestId = 15L
        val openedGroupBuyId = 100L
        val groupBuyRequest = GroupBuyRequest(userId = 1L, storeName = "성심당", productName = "튀김소보로",
            desiredQuantity = 2, desiredPickupDate = LocalDate.now().plusDays(5),
            status = GroupBuyRequestStatus.IN_CONTACT).apply { id = requestId }

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))
        `when`(groupBuyRepository.findWithStoreById(openedGroupBuyId)).thenReturn(Optional.empty())

        val ex = assertThrows<CustomException> {
            service.updateStatus(
                requestId,
                GroupBuyRequestStatusUpdateRequest(
                    targetStatus = GroupBuyRequestStatus.OPENED,
                    openedGroupBuyId = openedGroupBuyId
                )
            )
        }

        assertEquals(ErrorCode.GROUPBUY_NOT_FOUND, ex.errorCode)
        verify(groupBuyRequestStatusHistoryRepository, never()).save(any())
        verifyNoInteractions(groupBuyOpenRequestService)
    }

    @Test
    fun `허용되지 않는 상태 전이는 GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION 예외`() {
        val requestId = 16L
        val groupBuyRequest = GroupBuyRequest(userId = 1L, storeName = "성심당", productName = "튀김소보로",
            desiredQuantity = 2, desiredPickupDate = LocalDate.now().plusDays(5)).apply { id = requestId }

        `when`(groupBuyRequestRepository.findById(requestId)).thenReturn(Optional.of(groupBuyRequest))

        val ex = assertThrows<CustomException> {
            service.updateStatus(
                requestId,
                GroupBuyRequestStatusUpdateRequest(targetStatus = GroupBuyRequestStatus.OPENED)
            )
        }

        assertEquals(ErrorCode.GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION, ex.errorCode)
        verify(groupBuyRequestStatusHistoryRepository, never()).save(any())
    }

    private inline fun <reified T> argumentCaptor() = org.mockito.ArgumentCaptor.forClass(T::class.java)
}
