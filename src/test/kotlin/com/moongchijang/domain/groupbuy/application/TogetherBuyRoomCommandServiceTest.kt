package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyRoomCreateRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.application.StorePickupPolicyService
import com.moongchijang.domain.store.application.dto.StorePickupAvailabilityResponse
import com.moongchijang.domain.store.application.dto.StorePickupDateAvailabilityResponse
import com.moongchijang.domain.store.application.dto.StorePickupSlotAvailabilityResponse
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class TogetherBuyRoomCommandServiceTest {
    private val groupBuyRepository: GroupBuyRepository = mock(GroupBuyRepository::class.java)
    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val productRepository: ProductRepository = mock(ProductRepository::class.java)
    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val pickupPolicyService: StorePickupPolicyService = mock(StorePickupPolicyService::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-06-01T01:00:00Z"), ZoneOffset.UTC)
    private val service = TogetherBuyRoomCommandService(
        groupBuyRepository,
        participationRepository,
        productRepository,
        userRepository,
        pickupPolicyService,
        clock,
    )

    @Test
    fun `총대 참여와 상품 스냅샷을 포함해 함께 사기 방을 생성한다`() {
        val product = product()
        val organizer = UserFixture.createKakaoUser()
        `when`(userRepository.findByIdAndDeletedAtIsNull(organizer.id!!)).thenReturn(organizer)
        `when`(productRepository.findWithStoreById(product.id)).thenReturn(product)
        `when`(pickupPolicyService.getAvailability(product.store.id, PICKUP_DATE, PICKUP_DATE)).thenReturn(availability())
        `when`(groupBuyRepository.save(any(GroupBuy::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as GroupBuy).apply { id = 55L }
        }

        val result = service.create(organizer.id!!, request())

        assertThat(result.groupBuyId).isEqualTo(55L)
        assertThat(result.status.name).isEqualTo("RECRUITING")
        assertThat(result.progress.totalQuantity).isEqualTo(2)
        assertThat(result.paymentDeadline).isEqualTo(PAYMENT_DEADLINE)
        val roomCaptor = ArgumentCaptor.forClass(GroupBuy::class.java)
        verify(groupBuyRepository).save(roomCaptor.capture())
        assertThat(roomCaptor.value.status).isEqualTo(GroupBuyStatus.IN_PROGRESS)
        assertThat(roomCaptor.value.product).isSameAs(product)
        assertThat(roomCaptor.value.organizer).isSameAs(organizer)
        assertThat(roomCaptor.value.price).isEqualTo(product.unitPrice)
        val participationCaptor = ArgumentCaptor.forClass(Participation::class.java)
        verify(participationRepository).save(participationCaptor.capture())
        assertThat(participationCaptor.value.user).isSameAs(organizer)
        assertThat(participationCaptor.value.quantity).isEqualTo(2)
        assertThat(participationCaptor.value.totalAmount).isEqualTo(12_000)
    }

    @Test
    fun `상품이 요청 매장에 속하지 않으면 방을 만들 수 없다`() {
        val product = product()
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(UserFixture.createKakaoUser())
        `when`(productRepository.findWithStoreById(product.id)).thenReturn(product)

        val exception = assertThrows<CustomException> {
            service.create(1L, request(storeId = 999L))
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.GROUPBUY_ROOM_PRODUCT_NOT_AVAILABLE)
    }

    @Test
    fun `선택한 픽업 시간대가 더 이상 가능하지 않으면 방을 만들 수 없다`() {
        val product = product()
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(UserFixture.createKakaoUser())
        `when`(productRepository.findWithStoreById(product.id)).thenReturn(product)
        `when`(pickupPolicyService.getAvailability(product.store.id, PICKUP_DATE, PICKUP_DATE)).thenReturn(
            availability(slotSelectable = false),
        )

        val exception = assertThrows<CustomException> {
            service.create(1L, request())
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.GROUPBUY_ROOM_PICKUP_NOT_AVAILABLE)
    }

    @Test
    fun `단독 구매 방은 총대 수량만으로 최소 수량을 만족해야 한다`() {
        val product = product()
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(UserFixture.createKakaoUser())
        `when`(productRepository.findWithStoreById(product.id)).thenReturn(product)

        val exception = assertThrows<CustomException> {
            service.create(1L, request(organizerQuantity = 2, isSoloRoom = true))
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.GROUPBUY_ROOM_INVALID_QUANTITY)
    }

    @Test
    fun `재판매 금지 미동의면 방을 만들 수 없다`() {
        val exception = assertThrows<CustomException> {
            service.create(1L, request(resaleProhibitionAgreed = false))
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_INPUT)
    }

    private fun request(
        storeId: Long = 10L,
        organizerQuantity: Int = 2,
        isSoloRoom: Boolean = false,
        resaleProhibitionAgreed: Boolean = true,
    ) = TogetherBuyRoomCreateRequest(
        storeId = storeId,
        storeProductId = 20L,
        pickupDate = PICKUP_DATE,
        pickupSlotStart = LocalTime.of(14, 0),
        pickupSlotEnd = LocalTime.of(15, 0),
        organizerQuantity = organizerQuantity,
        resaleProhibitionAgreed = resaleProhibitionAgreed,
        isSoloRoom = isSoloRoom,
    )

    private fun product() = Product(
        store = Store(
            name = "테스트 매장",
            address = "서울 성동구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_ALL,
        ).also { it.id = 10L },
        name = "테스트 상품",
        description = "테스트 설명",
        unitPrice = 6_000,
        minQuantity = 5,
        maxQuantity = 20,
        preparationDays = 2,
        storageType = ProductStorageType.ROOM_TEMPERATURE,
        status = ProductStatus.ACTIVE,
        thumbnailKey = "products/test.jpg",
    ).also { it.id = 20L }

    private fun availability(slotSelectable: Boolean = true) = StorePickupAvailabilityResponse(
        storeId = 10L,
        preparationDays = 2,
        maxAdvanceDays = 14,
        dates = listOf(
            StorePickupDateAvailabilityResponse(
                date = PICKUP_DATE,
                selectable = slotSelectable,
                unavailableReason = null,
                paymentDeadline = if (slotSelectable) PAYMENT_DEADLINE else null,
                sameDayPaymentWarning = false,
                slots = listOf(
                    StorePickupSlotAvailabilityResponse(
                        startTime = LocalTime.of(14, 0),
                        endTime = LocalTime.of(15, 0),
                        selectable = slotSelectable,
                        unavailableReason = null,
                        remainingQuantity = null,
                    ),
                ),
            ),
        ),
    )

    companion object {
        private val PICKUP_DATE = LocalDate.of(2026, 6, 5)
        private val PAYMENT_DEADLINE = LocalDateTime.of(2026, 6, 3, 23, 59)
    }
}
