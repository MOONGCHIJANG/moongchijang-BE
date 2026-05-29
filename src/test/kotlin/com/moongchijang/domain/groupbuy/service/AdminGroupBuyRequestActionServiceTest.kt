package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.AdminGroupBuyRequestActionService
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestApproveRequest
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestRejectRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyImage
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyImageRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.repository.StoreRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.springframework.context.ApplicationEventPublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.lenient
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminGroupBuyRequestActionServiceTest {

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    @Mock
    private lateinit var groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var groupBuyImageRepository: GroupBuyImageRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var s3ImageReferenceResolver: S3ImageReferenceResolver

    @InjectMocks
    private lateinit var service: AdminGroupBuyRequestActionService

    @BeforeEach
    fun setUp() {
        lenient().`when`(s3ImageReferenceResolver.resolve("https://cdn.example.com/1.jpg"))
            .thenReturn(S3ImageReferenceResolver.ResolvedImageReference("exhibition/1.jpg", "https://cdn.example.com/1.jpg"))
        lenient().`when`(s3ImageReferenceResolver.resolve("https://cdn.example.com/2.jpg"))
            .thenReturn(S3ImageReferenceResolver.ResolvedImageReference("exhibition/2.jpg", "https://cdn.example.com/2.jpg"))
    }

    @Test
    fun `소비자 요청을 승인하면 매장과 공구와 이미지를 생성하고 OPENED로 전환한다`() {
        val requestId = 10L
        val groupBuyRequest = groupBuyRequest(status = GroupBuyRequestStatus.IN_REVIEW).apply { id = requestId }
        val store = store(id = 20L)
        val approveRequest = approveRequest()

        `when`(groupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(groupBuyRequest))
        `when`(
            storeRepository.findFirstByNameIgnoreCaseAndAddressIgnoreCase(
                "뭉치장 베이커리",
                "서울 성동구 성수이로 1"
            )
        ).thenReturn(null)
        `when`(storeRepository.save(any(Store::class.java))).thenReturn(store)
        `when`(groupBuyRepository.save(any(GroupBuy::class.java))).thenAnswer {
            (it.arguments[0] as GroupBuy).apply { id = 30L }
        }
        `when`(s3ImageReferenceResolver.resolve("https://cdn.example.com/1.jpg"))
            .thenReturn(S3ImageReferenceResolver.ResolvedImageReference("exhibition/1.jpg", "https://cdn.example.com/1.jpg"))
        `when`(s3ImageReferenceResolver.resolve("https://cdn.example.com/2.jpg"))
            .thenReturn(S3ImageReferenceResolver.ResolvedImageReference("exhibition/2.jpg", "https://cdn.example.com/2.jpg"))

        val result = service.approve(requestId, approveRequest)

        assertEquals(requestId, result.requestId)
        assertEquals(GroupBuyRequestStatus.OPENED, result.status)
        assertEquals(30L, result.groupBuyId)
        assertEquals(GroupBuyRequestStatus.OPENED, groupBuyRequest.status)
        assertEquals(30L, groupBuyRequest.openedGroupBuyId)
        assertNull(groupBuyRequest.rejectionReason)

        val groupBuyCaptor = argumentCaptor<GroupBuy>()
        verify(groupBuyRepository).save(groupBuyCaptor.capture())
        assertEquals("두쫀쿠 세트", groupBuyCaptor.value.productName)
        assertEquals(12000, groupBuyCaptor.value.originalPrice)
        assertEquals(9900, groupBuyCaptor.value.price)
        assertEquals(20, groupBuyCaptor.value.targetQuantity)
        assertEquals(50, groupBuyCaptor.value.maxQuantity)
        assertEquals(2, groupBuyCaptor.value.perUserLimit)
        assertEquals("https://cdn.example.com/1.jpg", groupBuyCaptor.value.thumbnailUrl)
        assertEquals("exhibition/1.jpg", groupBuyCaptor.value.thumbnailKey)
        assertEquals("01012345678", groupBuyCaptor.value.pickupContact)
        assertEquals(approveRequest.recruitmentStartAt, groupBuyCaptor.value.recruitmentStartAt)

        val imageCaptor = argumentCaptor<Iterable<GroupBuyImage>>()
        verify(groupBuyImageRepository).saveAll(imageCaptor.capture())
        assertEquals(2, imageCaptor.value.toList().size)
        assertEquals(listOf("exhibition/1.jpg", "exhibition/2.jpg"), imageCaptor.value.toList().map { it.imageKey })
        verify(groupBuyRequestStatusHistoryRepository).save(any())
        verify(eventPublisher).publishEvent(AdminGroupBuyRequestActionService.AdminGroupBuyOpenedEvent(30L))
    }

    @Test
    fun `기존 매장 ID가 있으면 매장을 새로 만들지 않는다`() {
        val requestId = 11L
        val groupBuyRequest = groupBuyRequest(status = GroupBuyRequestStatus.IN_CONTACT).apply { id = requestId }
        val store = store(id = 21L)

        `when`(groupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(groupBuyRequest))
        `when`(storeRepository.findById(21L)).thenReturn(Optional.of(store))
        `when`(groupBuyRepository.save(any(GroupBuy::class.java))).thenAnswer {
            (it.arguments[0] as GroupBuy).apply { id = 31L }
        }

        service.approve(requestId, approveRequest(storeId = 21L))

        verify(storeRepository, never()).save(any(Store::class.java))
        verify(storeRepository).findById(21L)
    }

    @Test
    fun `매장 ID가 없어도 같은 이름과 주소의 기존 매장이 있으면 재사용한다`() {
        val requestId = 15L
        val groupBuyRequest = groupBuyRequest(status = GroupBuyRequestStatus.IN_REVIEW).apply { id = requestId }
        val existingStore = store(id = 22L)

        `when`(groupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(groupBuyRequest))
        `when`(
            storeRepository.findFirstByNameIgnoreCaseAndAddressIgnoreCase(
                "뭉치장 베이커리",
                "서울 성동구 성수이로 1"
            )
        ).thenReturn(existingStore)
        `when`(groupBuyRepository.save(any(GroupBuy::class.java))).thenAnswer {
            (it.arguments[0] as GroupBuy).apply { id = 32L }
        }

        service.approve(requestId, approveRequest())

        val groupBuyCaptor = argumentCaptor<GroupBuy>()
        verify(groupBuyRepository).save(groupBuyCaptor.capture())
        assertEquals(existingStore, groupBuyCaptor.value.store)
        verify(storeRepository, never()).save(any(Store::class.java))
    }

    @Test
    fun `이미 승인된 요청은 다시 승인할 수 없다`() {
        val requestId = 12L
        val groupBuyRequest = groupBuyRequest(status = GroupBuyRequestStatus.OPENED).apply { id = requestId }
        `when`(groupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(groupBuyRequest))

        val ex = assertThrows<CustomException> {
            service.approve(requestId, approveRequest())
        }

        assertEquals(ErrorCode.GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION, ex.errorCode)
        verify(groupBuyRepository, never()).save(any(GroupBuy::class.java))
    }

    @Test
    fun `공구가가 정가보다 크면 승인할 수 없다`() {
        val requestId = 13L
        val groupBuyRequest = groupBuyRequest(status = GroupBuyRequestStatus.IN_REVIEW).apply { id = requestId }
        `when`(groupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(groupBuyRequest))

        val ex = assertThrows<CustomException> {
            service.approve(requestId, approveRequest(originalPrice = 9000, price = 9900))
        }

        assertEquals(ErrorCode.GROUPBUY_REQUEST_APPROVAL_INVALID_PRICE, ex.errorCode)
    }

    @Test
    fun `픽업일이 모집 마감일과 같으면 승인할 수 없다`() {
        val requestId = 16L
        val groupBuyRequest = groupBuyRequest(status = GroupBuyRequestStatus.IN_REVIEW).apply { id = requestId }
        val deadline = LocalDateTime.now().plusDays(3)
        `when`(groupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(groupBuyRequest))

        val ex = assertThrows<CustomException> {
            service.approve(
                requestId,
                approveRequest(deadline = deadline, pickupDate = deadline.toLocalDate())
            )
        }

        assertEquals(ErrorCode.GROUPBUY_REQUEST_APPROVAL_INVALID_PICKUP, ex.errorCode)
    }

    @Test
    fun `지역과 세부지역이 맞지 않으면 전용 예외를 던진다`() {
        val requestId = 17L
        val groupBuyRequest = groupBuyRequest(status = GroupBuyRequestStatus.IN_REVIEW).apply { id = requestId }
        `when`(groupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(groupBuyRequest))

        val ex = assertThrows<CustomException> {
            service.approve(
                requestId,
                approveRequest(region = RegionType.GYEONGGI, district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN)
            )
        }

        assertEquals(ErrorCode.GROUPBUY_REQUEST_APPROVAL_STORE_REGION_MISMATCH, ex.errorCode)
    }

    @Test
    fun `소비자 요청을 반려하면 사유와 REJECTED 상태를 저장한다`() {
        val requestId = 14L
        val groupBuyRequest = groupBuyRequest(status = GroupBuyRequestStatus.IN_REVIEW).apply { id = requestId }
        `when`(groupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(groupBuyRequest))

        val result = service.reject(requestId, AdminGroupBuyRequestRejectRequest("  재고 확보 불가  "))

        assertEquals(GroupBuyRequestStatus.REJECTED, result.status)
        assertEquals(GroupBuyRequestStatus.REJECTED, groupBuyRequest.status)
        assertEquals("재고 확보 불가", groupBuyRequest.rejectionReason)
        assertNull(groupBuyRequest.openedGroupBuyId)
        verify(groupBuyRequestStatusHistoryRepository).save(any())
    }

    private fun groupBuyRequest(status: GroupBuyRequestStatus): GroupBuyRequest =
        GroupBuyRequest(
            userId = 1L,
            storeName = "뭉치장 베이커리",
            storeAddress = "서울 성동구 성수이로 1",
            productName = "두쫀쿠",
            desiredQuantity = 2,
            desiredPickupDate = LocalDate.now().plusDays(7),
            status = status
        )

    private fun store(id: Long): Store =
        Store(
            name = "뭉치장 베이커리",
            address = "서울 성동구 성수이로 1",
            phoneNumber = "01012345678",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN
        ).apply { this.id = id }

    private fun approveRequest(
        storeId: Long? = null,
        originalPrice: Int = 12000,
        price: Int = 9900,
        region: RegionType = RegionType.SEOUL,
        district: DistrictType = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN,
        deadline: LocalDateTime = LocalDateTime.now().plusDays(3),
        pickupDate: LocalDate = LocalDate.now().plusDays(5),
    ): AdminGroupBuyRequestApproveRequest =
        AdminGroupBuyRequestApproveRequest(
            storeId = storeId,
            storeName = "뭉치장 베이커리",
            storeAddress = "서울 성동구 성수이로 1",
            storePhoneNumber = "01012345678",
            region = region,
            district = district,
            productName = "두쫀쿠 세트",
            productDescription = "소금빵 포함",
            originalPrice = originalPrice,
            price = price,
            targetQuantity = 20,
            maxQuantity = 50,
            perUserLimit = 2,
            imageUrls = listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"),
            recruitmentStartAt = LocalDateTime.now().minusHours(1),
            deadline = deadline,
            pickupDate = pickupDate,
            pickupTimeStart = LocalTime.of(12, 0),
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "서울 성동구 성수이로 1",
            pickupContact = "01012345678"
        )

    private inline fun <reified T> argumentCaptor() = org.mockito.ArgumentCaptor.forClass(T::class.java)
}
