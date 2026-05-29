package com.moongchijang.domain.owner.application

import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestImage
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestImageRepository
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestRepository
import com.moongchijang.domain.store.domain.repository.StoreRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.lenient
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OwnerGroupBuyRequestServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var storeStaffRepository: StoreStaffRepository

    @Mock
    private lateinit var ownerGroupBuyRequestRepository: OwnerGroupBuyRequestRepository

    @Mock
    private lateinit var ownerGroupBuyRequestImageRepository: OwnerGroupBuyRequestImageRepository

    @Mock
    private lateinit var s3ImageReferenceResolver: S3ImageReferenceResolver

    private val service: OwnerGroupBuyRequestService by lazy {
        OwnerGroupBuyRequestService(
            userRepository = userRepository,
            storeRepository = storeRepository,
            storeStaffRepository = storeStaffRepository,
            ownerGroupBuyRequestRepository = ownerGroupBuyRequestRepository,
            ownerGroupBuyRequestImageRepository = ownerGroupBuyRequestImageRepository,
            clock = FIXED_CLOCK,
            s3ImageReferenceResolver = s3ImageReferenceResolver,
        )
    }

    @BeforeEach
    fun setUp() {
        lenient().`when`(s3ImageReferenceResolver.resolve("https://cdn.example.com/1.jpg"))
            .thenReturn(S3ImageReferenceResolver.ResolvedImageReference("1.jpg", "https://cdn.example.com/1.jpg"))
        lenient().`when`(s3ImageReferenceResolver.resolve("https://cdn.example.com/2.jpg"))
            .thenReturn(S3ImageReferenceResolver.ResolvedImageReference("2.jpg", "https://cdn.example.com/2.jpg"))
        lenient().`when`(s3ImageReferenceResolver.resolveForRead(anyString())).thenAnswer { key ->
            "https://cdn.example.com/${key.arguments[0] as String}"
        }
    }

    @Test
    fun `사장님이 본인 매장 공구 개설 요청을 제출하면 PENDING 요청과 이미지를 저장한다`() {
        val owner = seller()
        val store = GroupBuyFixture.createStore()
        val request = validRequest()

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeRepository.findById(request.storeId)).thenReturn(Optional.of(store))
        `when`(storeStaffRepository.existsByUserIdAndStoreId(owner.id!!, request.storeId)).thenReturn(true)
        `when`(ownerGroupBuyRequestRepository.save(any(OwnerGroupBuyRequest::class.java))).thenAnswer {
            (it.arguments[0] as OwnerGroupBuyRequest).apply { id = 101L }
        }

        val result = service.create(owner.id!!, request)

        assertEquals(101L, result.requestId)
        assertEquals(OwnerGroupBuyRequestStatus.PENDING, result.status)

        val requestCaptor = argumentCaptor<OwnerGroupBuyRequest>()
        verify(ownerGroupBuyRequestRepository).save(requestCaptor.capture())
        assertEquals(owner, requestCaptor.value.owner)
        assertEquals(store, requestCaptor.value.store)
        assertEquals("두쫀쿠 세트", requestCaptor.value.productName)
        assertEquals("1.jpg", requestCaptor.value.thumbnailKey)

        val imageCaptor = argumentCaptor<Iterable<OwnerGroupBuyRequestImage>>()
        verify(ownerGroupBuyRequestImageRepository).saveAll(imageCaptor.capture())
        val images = imageCaptor.value.toList()
        assertEquals(2, images.size)
        assertEquals(listOf(0, 1), images.map { it.sortOrder })
        assertEquals(listOf("1.jpg", "2.jpg"), images.map { it.imageKey })
        assertTrue(images.all { it.request.id == 101L })
    }

    @Test
    fun `사장님 본인 매장의 공구 개설 요청 목록을 조회한다`() {
        val owner = seller()
        val pageable = PageRequest.of(0, 20)
        val request = ownerRequest(owner = owner).apply {
            id = 101L
            status = OwnerGroupBuyRequestStatus.REJECTED
            rejectionReason = "매장 사정으로 어렵습니다"
        }

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(listOf(1L))
        `when`(ownerGroupBuyRequestRepository.findPageByOwnerIdAndStoreIdIn(owner.id!!, listOf(1L), pageable))
            .thenReturn(PageImpl(listOf(request), pageable, 1))

        val result = service.getMyRequests(owner.id!!, pageable)

        assertEquals(1, result.content.size)
        assertEquals(1, result.totalElements)
        assertEquals(1, result.totalPages)
        assertEquals(0, result.number)
        assertEquals(20, result.size)
        assertEquals(101L, result.content[0].requestId)
        assertEquals("두쫀쿠 세트", result.content[0].productName)
        assertEquals("뭉치장 베이커리", result.content[0].storeName)
        assertEquals(12000, result.content[0].originalPrice)
        assertEquals(9900, result.content[0].price)
        assertEquals(20, result.content[0].targetQuantity)
        assertEquals(OwnerGroupBuyRequestStatus.REJECTED, result.content[0].status)
        assertEquals("매장 사정으로 어렵습니다", result.content[0].rejectionReason)
    }

    @Test
    fun `사장님 소속 매장이 없으면 공구 개설 요청 목록은 빈 목록이다`() {
        val owner = seller()
        val pageable = PageRequest.of(0, 20)

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(emptyList())

        val result = service.getMyRequests(owner.id!!, pageable)

        assertEquals(emptyList<Any>(), result.content)
        assertEquals(0, result.totalElements)
        assertEquals(0, result.totalPages)
        assertEquals(0, result.number)
        assertEquals(20, result.size)
        verifyNoInteractions(ownerGroupBuyRequestRepository)
    }

    @Test
    fun `사장님 공구 개설 요청 상세를 조회한다`() {
        val owner = seller()
        val request = ownerRequest(owner = owner).apply { id = 101L }
        val images = listOf(
            OwnerGroupBuyRequestImage(request = request, imageKey = "1.jpg", sortOrder = 0),
            OwnerGroupBuyRequestImage(request = request, imageKey = "2.jpg", sortOrder = 1)
        )

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(ownerGroupBuyRequestRepository.findById(101L)).thenReturn(Optional.of(request))
        `when`(storeStaffRepository.existsByUserIdAndStoreId(owner.id!!, 1L)).thenReturn(true)
        `when`(ownerGroupBuyRequestImageRepository.findAllByRequestIdOrderBySortOrderAsc(101L)).thenReturn(images)

        val result = service.getDetail(owner.id!!, 101L)

        assertEquals(101L, result.requestId)
        assertEquals(1L, result.storeId)
        assertEquals("뭉치장 베이커리", result.storeName)
        assertEquals("두쫀쿠 세트", result.productName)
        assertEquals("소금빵 포함", result.productDescription)
        assertEquals(12000, result.originalPrice)
        assertEquals(9900, result.price)
        assertEquals(20, result.targetQuantity)
        assertEquals(50, result.maxQuantity)
        assertEquals(2, result.perUserLimit)
        assertEquals("https://cdn.example.com/1.jpg", result.thumbnailUrl)
        assertEquals(listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"), result.imageUrls)
        assertEquals(OwnerGroupBuyRequestStatus.PENDING, result.status)
    }

    @Test
    fun `본인 요청이 아니면 사장님 공구 개설 요청 상세를 조회할 수 없다`() {
        val owner = seller()
        val otherOwner = seller().apply { id = 2L }
        val request = ownerRequest(owner = otherOwner).apply { id = 101L }

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(ownerGroupBuyRequestRepository.findById(101L)).thenReturn(Optional.of(request))

        val ex = assertThrows<CustomException> {
            service.getDetail(owner.id!!, 101L)
        }

        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
        verifyNoInteractions(ownerGroupBuyRequestImageRepository)
    }

    @Test
    fun `구매자 계정이면 사장님 공구 개설 요청을 제출할 수 없다`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(user)

        val ex = assertThrows<CustomException> {
            service.create(1L, validRequest())
        }

        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
        verify(ownerGroupBuyRequestRepository, never()).save(any(OwnerGroupBuyRequest::class.java))
    }

    @Test
    fun `본인 매장이 아니면 사장님 공구 개설 요청을 제출할 수 없다`() {
        val owner = seller()
        val request = validRequest()

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeRepository.findById(request.storeId)).thenReturn(Optional.of(GroupBuyFixture.createStore()))
        `when`(storeStaffRepository.existsByUserIdAndStoreId(owner.id!!, request.storeId)).thenReturn(false)

        val ex = assertThrows<CustomException> {
            service.create(owner.id!!, request)
        }

        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
        verify(ownerGroupBuyRequestRepository, never()).save(any(OwnerGroupBuyRequest::class.java))
    }

    @Test
    fun `희망 공구 기간이 7일 미만이면 예외가 발생한다`() {
        val owner = seller()
        val request = validRequest(deadline = FIXED_NOW.plusDays(6))

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeRepository.findById(request.storeId)).thenReturn(Optional.of(GroupBuyFixture.createStore()))
        `when`(storeStaffRepository.existsByUserIdAndStoreId(owner.id!!, request.storeId)).thenReturn(true)

        val ex = assertThrows<CustomException> {
            service.create(owner.id!!, request)
        }

        assertEquals(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_DEADLINE, ex.errorCode)
    }

    @Test
    fun `최대 수량이 목표 수량보다 작으면 예외가 발생한다`() {
        val owner = seller()
        val request = validRequest(targetQuantity = 30, maxQuantity = 20)

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeRepository.findById(request.storeId)).thenReturn(Optional.of(GroupBuyFixture.createStore()))
        `when`(storeStaffRepository.existsByUserIdAndStoreId(owner.id!!, request.storeId)).thenReturn(true)

        val ex = assertThrows<CustomException> {
            service.create(owner.id!!, request)
        }

        assertEquals(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_QUANTITY, ex.errorCode)
    }

    @Test
    fun `픽업일이 공구 마감일과 같으면 예외가 발생한다`() {
        val owner = seller()
        val deadline = FIXED_NOW.plusDays(8)
        val request = validRequest(
            deadline = deadline,
            pickupDate = deadline.toLocalDate()
        )

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeRepository.findById(request.storeId)).thenReturn(Optional.of(GroupBuyFixture.createStore()))
        `when`(storeStaffRepository.existsByUserIdAndStoreId(owner.id!!, request.storeId)).thenReturn(true)

        val ex = assertThrows<CustomException> {
            service.create(owner.id!!, request)
        }

        assertEquals(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_PICKUP_DATE, ex.errorCode)
    }

    private fun seller() = UserFixture.createKakaoUser(id = 1L).apply {
        role = UserRole.SELLER
    }

    private fun ownerRequest(
        owner: com.moongchijang.domain.user.domain.entity.User = seller()
    ) = OwnerGroupBuyRequest(
        owner = owner,
        store = GroupBuyFixture.createStore(),
        productName = "두쫀쿠 세트",
        productDescription = "소금빵 포함",
        originalPrice = 12000,
        price = 9900,
        targetQuantity = 20,
        maxQuantity = 50,
        perUserLimit = 2,
        thumbnailKey = "1.jpg",
        deadline = FIXED_NOW.plusDays(8),
        pickupDate = FIXED_NOW.toLocalDate().plusDays(9),
        pickupTimeStart = LocalTime.of(12, 0),
        pickupTimeEnd = LocalTime.of(18, 0),
        pickupLocation = "서울 성동구 성수이로 1",
        pickupContact = "01012345678"
    )

    private fun validRequest(
        deadline: LocalDateTime = FIXED_NOW.plusDays(8),
        targetQuantity: Int = 20,
        maxQuantity: Int = 50,
        pickupDate: LocalDate = deadline.toLocalDate().plusDays(1)
    ) = OwnerGroupBuyRequestCreateRequest(
        storeId = 1L,
        productName = "두쫀쿠 세트",
        productDescription = "소금빵 포함",
        deadline = deadline,
        originalPrice = 12000,
        price = 9900,
        targetQuantity = targetQuantity,
        maxQuantity = maxQuantity,
        perUserLimit = 2,
        imageUrls = listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"),
        pickupDate = pickupDate,
        pickupTimeStart = LocalTime.of(12, 0),
        pickupTimeEnd = LocalTime.of(18, 0),
        pickupLocation = "서울 성동구 성수이로 1",
        pickupContact = "01012345678"
    )

    private inline fun <reified T> argumentCaptor() = org.mockito.ArgumentCaptor.forClass(T::class.java)

    private companion object {
        val FIXED_CLOCK: Clock = Clock.fixed(
            Instant.parse("2026-05-25T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
        )
        val FIXED_NOW: LocalDateTime = LocalDateTime.now(FIXED_CLOCK)
    }
}
