package com.moongchijang.domain.product.application

import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.product.application.dto.ProductFeedRequest
import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.product.domain.repository.ProductFeedCriteria
import com.moongchijang.domain.product.domain.repository.ProductFeedCursor
import com.moongchijang.domain.product.domain.repository.ProductFeedSort
import com.moongchijang.domain.product.domain.repository.ProductPickupDateCondition
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.entity.StorePickupTimeSlot
import com.moongchijang.domain.store.domain.repository.StoreHolidayRepository
import com.moongchijang.domain.store.domain.repository.StorePickupTimeSlotRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class ProductFeedQueryServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var storeHolidayRepository: StoreHolidayRepository

    @Mock
    private lateinit var storePickupTimeSlotRepository: StorePickupTimeSlotRepository

    @Mock
    private lateinit var favoriteRepository: FavoriteRepository

    @Mock
    private lateinit var s3ImageReferenceResolver: S3ImageReferenceResolver

    private val clock = Clock.fixed(Instant.parse("2026-10-25T15:00:00Z"), ZoneOffset.UTC)

    private lateinit var service: ProductFeedQueryService

    @BeforeEach
    fun setUp() {
        service = ProductFeedQueryService(
            productRepository = productRepository,
            storeHolidayRepository = storeHolidayRepository,
            storePickupTimeSlotRepository = storePickupTimeSlotRepository,
            favoriteRepository = favoriteRepository,
            s3ImageReferenceResolver = s3ImageReferenceResolver,
            clock = clock,
        )
    }

    @Test
    fun `필터를 조회 조건으로 변환하고 상품 카드와 다음 커서를 반환한다`() {
        val store = createStore()
        val first = createProduct(store, id = 1L, name = "첫 상품", unitPrice = 1_000)
        val second = createProduct(store, id = 2L, name = "둘째 상품", unitPrice = 2_000)
        val third = createProduct(store, id = 3L, name = "셋째 상품", unitPrice = 3_000)
        val expectedCriteria = ProductFeedCriteria(
            categoryCodes = setOf("FRESH_FOOD"),
            districts = DistrictType.findLeafByRegion(RegionType.SEOUL).toSet(),
            maxPreparationDays = 6,
            pickupDateConditions = (0L..6L).map { offset ->
                ProductPickupDateCondition(
                    date = LocalDate.of(2026, 10, 26).plusDays(offset),
                    maxPreparationDays = offset.toInt(),
                )
            },
            maxMinQuantity = 10,
            createdAtFrom = LocalDateTime.of(2026, 10, 19, 0, 0),
            sort = ProductFeedSort.PRICE_LOW,
        )
        `when`(productRepository.findFeed(expectedCriteria, null, 3))
            .thenReturn(listOf(first, second, third))
        `when`(favoriteRepository.findProductIdsByUserIdAndProductIdIn(99L, listOf(1L, 2L)))
            .thenReturn(setOf(1L))
        `when`(storeHolidayRepository.findAllByStoreIdInAndActiveTrue(setOf(store.id)))
            .thenReturn(emptyList())
        `when`(
            storePickupTimeSlotRepository
                .findAllByStoreIdInAndActiveTrueOrderByStoreIdAscDayOfWeekAscStartTimeAsc(setOf(store.id))
        ).thenReturn(
            listOf(
                StorePickupTimeSlot(
                    store = store,
                    dayOfWeek = 3,
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(11, 0),
                    id = 1L,
                )
            )
        )
        `when`(s3ImageReferenceResolver.resolveForRead("products/thumbnail.jpg"))
            .thenReturn("https://cdn.test/thumbnail.jpg")

        val result = service.getFeed(
            request = ProductFeedRequest(
                categoryCodes = setOf(" fresh_food "),
                districts = setOf(DistrictType.SEOUL_ALL),
                pickupThisWeek = true,
                smallQuantity = true,
                newOnly = true,
                sort = ProductFeedSort.PRICE_LOW,
                limit = 2,
            ),
            userId = 99L,
        )

        verify(productRepository).findFeed(expectedCriteria, null, 3)
        assertThat(result.items.map { it.productId }).containsExactly(1L, 2L)
        assertThat(result.items.first().isWishlisted).isTrue()
        assertThat(result.items.first().earliestPickupDate).isEqualTo(LocalDate.of(2026, 10, 28))
        assertThat(result.items.first().canPurchase).isTrue()
        assertThat(result.items.first().thumbnailUrl).isEqualTo("https://cdn.test/thumbnail.jpg")
        assertThat(result.hasNext).isTrue()
        assertThat(result.nextCursor).isEqualTo(ProductFeedCursor.PriceLow(2_000, 2L).encode())
    }

    @Test
    fun `같은 지역의 전체와 하위 지역을 동시에 선택하면 예외를 발생시킨다`() {
        val request = ProductFeedRequest(
            districts = setOf(
                DistrictType.SEOUL_ALL,
                DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
            )
        )

        assertThrows<CustomException> { service.getFeed(request, null) }
    }

    private fun createStore(): Store = Store(
        name = "피드 테스트 매장",
        address = "서울 강남구",
        region = RegionType.SEOUL,
        district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        id = 10L,
    )

    private fun createProduct(
        store: Store,
        id: Long,
        name: String,
        unitPrice: Int,
    ): Product = Product(
        store = store,
        categoryCode = "FRESH_FOOD",
        name = name,
        description = "$name 설명",
        unitPrice = unitPrice,
        minQuantity = 10,
        maxQuantity = 100,
        preparationDays = 2,
        storageType = ProductStorageType.ROOM_TEMPERATURE,
        thumbnailKey = "products/thumbnail.jpg",
        id = id,
    )
}
