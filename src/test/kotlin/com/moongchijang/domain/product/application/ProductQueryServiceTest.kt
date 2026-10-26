package com.moongchijang.domain.product.application

import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.product.application.dto.ProductUnavailableReason
import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductImage
import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.product.domain.repository.ProductImageRepository
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.entity.StoreHoliday
import com.moongchijang.domain.store.domain.entity.StoreHolidayType
import com.moongchijang.domain.store.domain.entity.StorePickupTimeSlot
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import com.moongchijang.domain.store.domain.repository.StoreHolidayRepository
import com.moongchijang.domain.store.domain.repository.StorePickupTimeSlotRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.lenient
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class ProductQueryServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var productImageRepository: ProductImageRepository

    @Mock
    private lateinit var storeHolidayRepository: StoreHolidayRepository

    @Mock
    private lateinit var storePickupTimeSlotRepository: StorePickupTimeSlotRepository

    @Mock
    private lateinit var favoriteRepository: FavoriteRepository

    @Mock
    private lateinit var s3ImageReferenceResolver: S3ImageReferenceResolver

    private val clock = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZoneOffset.UTC)

    private lateinit var service: ProductQueryService

    @BeforeEach
    fun setUp() {
        lenient().`when`(s3ImageReferenceResolver.resolveForRead(anyString()))
            .thenAnswer { it.arguments[0] as String }
        service = ProductQueryService(
            productRepository = productRepository,
            productImageRepository = productImageRepository,
            storeHolidayRepository = storeHolidayRepository,
            storePickupTimeSlotRepository = storePickupTimeSlotRepository,
            favoriteRepository = favoriteRepository,
            s3ImageReferenceResolver = s3ImageReferenceResolver,
            clock = clock,
        )
    }

    @Test
    fun `상품 상세는 준비 기간과 휴무일을 반영한 픽업 가능일을 반환한다`() {
        val store = createStore()
        val product = createProduct(store)
        `when`(productRepository.findWithStoreById(product.id)).thenReturn(product)
        `when`(productImageRepository.findAllByProductIdOrderBySortOrderAscIdAsc(product.id)).thenReturn(
            listOf(
                ProductImage(product, "products/detail-1.jpg", 1, 1L),
                ProductImage(product, "products/detail-2.jpg", 2, 2L),
            )
        )
        `when`(storeHolidayRepository.findAllByStoreIdAndActiveTrue(store.id)).thenReturn(
            listOf(
                StoreHoliday(
                    store = store,
                    holidayType = StoreHolidayType.TEMPORARY,
                    holidayDate = LocalDate.of(2026, 10, 28),
                    reason = "임시 휴무",
                    id = 1L,
                )
            )
        )
        `when`(storePickupTimeSlotRepository.findAllByStoreIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(store.id))
            .thenReturn(
                listOf(
                    StorePickupTimeSlot(
                        store = store,
                        dayOfWeek = 2,
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(11, 0),
                        id = 1L,
                    ),
                    StorePickupTimeSlot(
                        store = store,
                        dayOfWeek = 3,
                        startTime = LocalTime.of(14, 0),
                        endTime = LocalTime.of(15, 0),
                        id = 2L,
                    ),
                )
            )

        val result = service.getDetail(product.id, null)

        assertThat(result.categoryCode).isEqualTo(Product.DEFAULT_CATEGORY_CODE)
        assertThat(result.pickup.availableFrom).isEqualTo(LocalDate.of(2026, 10, 27))
        assertThat(result.pickup.availableUntil).isEqualTo(LocalDate.of(2026, 11, 8))
        assertThat(result.pickup.availableDates.map { it.date }).containsExactly(
            LocalDate.of(2026, 10, 27),
            LocalDate.of(2026, 11, 3),
            LocalDate.of(2026, 11, 4),
        )
        assertThat(result.imageUrls).containsExactly("products/detail-1.jpg", "products/detail-2.jpg")
        assertThat(result.canPurchase).isTrue()
        assertThat(result.isWishlisted).isFalse()
        assertThat(result.unavailableReason).isNull()
    }

    @Test
    fun `판매 중지 매장의 상품은 상세를 제공하되 구매 불가로 반환한다`() {
        val store = createStore(salesStatus = StoreSalesStatus.PAUSED)
        val product = createProduct(store)
        `when`(productRepository.findWithStoreById(product.id)).thenReturn(product)
        `when`(productImageRepository.findAllByProductIdOrderBySortOrderAscIdAsc(product.id)).thenReturn(emptyList())
        `when`(storeHolidayRepository.findAllByStoreIdAndActiveTrue(store.id)).thenReturn(emptyList())
        `when`(storePickupTimeSlotRepository.findAllByStoreIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(store.id))
            .thenReturn(
                listOf(
                    StorePickupTimeSlot(
                        store = store,
                        dayOfWeek = 2,
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(11, 0),
                    )
                )
            )

        val result = service.getDetail(product.id, null)

        assertThat(result.canPurchase).isFalse()
        assertThat(result.unavailableReason).isEqualTo(ProductUnavailableReason.STORE_NOT_FOR_SALE)
    }

    @Test
    fun `상품이 존재하지 않으면 예외를 발생시킨다`() {
        `when`(productRepository.findWithStoreById(999L)).thenReturn(null)

        val exception = assertThrows<CustomException> { service.getDetail(999L, null) }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
    }

    @Test
    fun `로그인 사용자의 상품 찜 여부를 반환한다`() {
        val store = createStore()
        val product = createProduct(store)
        `when`(productRepository.findWithStoreById(product.id)).thenReturn(product)
        `when`(productImageRepository.findAllByProductIdOrderBySortOrderAscIdAsc(product.id)).thenReturn(emptyList())
        `when`(storeHolidayRepository.findAllByStoreIdAndActiveTrue(store.id)).thenReturn(emptyList())
        `when`(storePickupTimeSlotRepository.findAllByStoreIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(store.id))
            .thenReturn(emptyList())
        `when`(favoriteRepository.existsByUserIdAndProductId(30L, product.id)).thenReturn(true)

        val result = service.getDetail(product.id, 30L)

        assertThat(result.isWishlisted).isTrue()
    }

    private fun createStore(
        salesStatus: StoreSalesStatus = StoreSalesStatus.ACTIVE,
    ): Store = Store(
        name = "상세 테스트 매장",
        address = "서울 강남구",
        phoneNumber = "02-1234-5678",
        region = RegionType.SEOUL,
        district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        placeId = "naver-place-1",
        salesStatus = salesStatus,
        id = 10L,
    )

    private fun createProduct(store: Store): Product = Product(
        store = store,
        name = "테스트 상품",
        description = "상품 설명",
        unitPrice = 3_000,
        minQuantity = 10,
        maxQuantity = 100,
        preparationDays = 2,
        storageType = ProductStorageType.ROOM_TEMPERATURE,
        thumbnailKey = "products/thumbnail.jpg",
        id = 20L,
    )
}
