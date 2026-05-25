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
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
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

    private val service: OwnerGroupBuyRequestService by lazy {
        OwnerGroupBuyRequestService(
            userRepository = userRepository,
            storeRepository = storeRepository,
            storeStaffRepository = storeStaffRepository,
            ownerGroupBuyRequestRepository = ownerGroupBuyRequestRepository,
            ownerGroupBuyRequestImageRepository = ownerGroupBuyRequestImageRepository,
            clock = FIXED_CLOCK
        )
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
        assertEquals("https://cdn.example.com/1.jpg", requestCaptor.value.thumbnailUrl)

        val imageCaptor = argumentCaptor<Iterable<OwnerGroupBuyRequestImage>>()
        verify(ownerGroupBuyRequestImageRepository).saveAll(imageCaptor.capture())
        val images = imageCaptor.value.toList()
        assertEquals(2, images.size)
        assertEquals(listOf(0, 1), images.map { it.sortOrder })
        assertEquals(listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"), images.map { it.imageUrl })
        assertTrue(images.all { it.request.id == 101L })
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
