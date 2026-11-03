package com.moongchijang.domain.product.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.product.domain.repository.ProductFeedCriteria
import com.moongchijang.domain.product.domain.repository.ProductFeedCursor
import com.moongchijang.domain.product.domain.repository.ProductFeedSort
import com.moongchijang.domain.product.domain.repository.ProductPickupDateCondition
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.entity.StoreHoliday
import com.moongchijang.domain.store.domain.entity.StoreHolidayType
import com.moongchijang.domain.store.domain.entity.StorePickupTimeSlot
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductFeedRepositoryIntegrationTest {

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var em: EntityManager

    @Test
    fun `판매 가능한 상품을 카테고리와 지역으로 필터링한다`() {
        val seoulStore = persistStore(
            name = "서울 매장",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        )
        val busanStore = persistStore(
            name = "부산 매장",
            region = RegionType.BUSAN,
            district = DistrictType.BUSAN_HAEUNDAE_CENTUM_MARINE_CITY,
        )
        val pausedStore = persistStore(
            name = "판매 중지 매장",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
            salesStatus = StoreSalesStatus.PAUSED,
        )
        val expected = persistProduct(seoulStore, "서울 신선식품", categoryCode = "FRESH_FOOD")
        persistProduct(seoulStore, "서울 음료", categoryCode = "BEVERAGE")
        persistProduct(busanStore, "부산 신선식품", categoryCode = "FRESH_FOOD")
        persistProduct(pausedStore, "중지 매장 신선식품", categoryCode = "FRESH_FOOD")
        persistProduct(
            seoulStore,
            "비활성 신선식품",
            categoryCode = "FRESH_FOOD",
            status = ProductStatus.INACTIVE,
        )
        flushAndClear()

        val products = productRepository.findFeed(
            criteria = ProductFeedCriteria(
                categoryCodes = setOf(" fresh_food "),
                districts = setOf(DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG),
            ),
            cursor = null,
            limit = 20,
        )

        assertThat(products.map { it.id }).containsExactly(expected.id)
        assertThat(products.single().store.name).isEqualTo("서울 매장")
    }

    @Test
    fun `픽업 준비 기간과 최소 수량 조건으로 필터링한다`() {
        val store = persistStore(
            name = "필터 매장",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        )
        val expected = persistProduct(
            store = store,
            name = "이번 주 소량 상품",
            preparationDays = 2,
            minQuantity = 10,
        )
        persistProduct(store, "준비 기간 초과 상품", preparationDays = 7, minQuantity = 10)
        persistProduct(store, "최소 수량 초과 상품", preparationDays = 2, minQuantity = 20)
        flushAndClear()

        val products = productRepository.findFeed(
            criteria = ProductFeedCriteria(
                maxPreparationDays = 3,
                maxMinQuantity = 10,
            ),
            cursor = null,
            limit = 20,
        )

        assertThat(products.map { it.id }).containsExactly(expected.id)
    }

    @Test
    fun `검색 후보 상품 ID 안에서만 피드를 조회한다`() {
        val store = persistStore(
            name = "검색 매장",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        )
        val expected = persistProduct(store, "검색 대상 상품")
        persistProduct(store, "검색 제외 상품")
        flushAndClear()

        val products = productRepository.findFeed(
            criteria = ProductFeedCriteria(productIds = setOf(expected.id)),
            cursor = null,
            limit = 20,
        )

        assertThat(products.map { it.id }).containsExactly(expected.id)
    }

    @Test
    fun `이번 주 실제 픽업 시간대가 있고 휴무가 아닌 상품만 조회한다`() {
        val availableStore = persistStore(
            name = "수요일 픽업 매장",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        )
        val holidayStore = persistStore(
            name = "수요일 휴무 매장",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        )
        val noSlotStore = persistStore(
            name = "픽업 시간대 없는 매장",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        )
        val expected = persistProduct(availableStore, "픽업 가능 상품", preparationDays = 2)
        persistProduct(holidayStore, "휴무 상품", preparationDays = 2)
        persistProduct(noSlotStore, "시간대 없는 상품", preparationDays = 2)
        listOf(availableStore, holidayStore).forEach { store ->
            em.persist(
                StorePickupTimeSlot(
                    store = store,
                    dayOfWeek = 3,
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(11, 0),
                )
            )
        }
        em.persist(
            StoreHoliday(
                store = holidayStore,
                holidayType = StoreHolidayType.TEMPORARY,
                holidayDate = LocalDate.of(2026, 10, 28),
            )
        )
        flushAndClear()

        val products = productRepository.findFeed(
            criteria = ProductFeedCriteria(
                pickupDateConditions = listOf(
                    ProductPickupDateCondition(
                        date = LocalDate.of(2026, 10, 28),
                        maxPreparationDays = 2,
                    )
                )
            ),
            cursor = null,
            limit = 20,
        )

        assertThat(products.map { it.id }).containsExactly(expected.id)
    }

    @Test
    fun `가격순 커서 다음 상품을 중복 없이 조회한다`() {
        val store = persistStore(
            name = "커서 매장",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        )
        persistProduct(store, "천 원 상품", unitPrice = 1_000)
        val second = persistProduct(store, "이천 원 상품", unitPrice = 2_000)
        val third = persistProduct(store, "삼천 원 상품", unitPrice = 3_000)
        flushAndClear()

        val criteria = ProductFeedCriteria(sort = ProductFeedSort.PRICE_LOW)
        val firstPage = productRepository.findFeed(criteria, cursor = null, limit = 2)
        val secondPage = productRepository.findFeed(
            criteria = criteria,
            cursor = ProductFeedCursor.PriceLow(unitPrice = second.unitPrice, id = second.id),
            limit = 2,
        )

        assertThat(firstPage.map { it.unitPrice }).containsExactly(1_000, 2_000)
        assertThat(secondPage.map { it.id }).containsExactly(third.id)
    }

    @Test
    fun `진행 중인 함께 사기 방 수가 많은 상품부터 조회하고 커서를 적용한다`() {
        val store = persistStore(
            name = "인기 정렬 매장",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        )
        val popular = persistProduct(store, "인기 상품")
        val second = persistProduct(store, "두 번째 상품")
        persistProduct(store, "방 없는 상품")
        repeat(2) { persistGroupBuy(store, popular) }
        persistGroupBuy(store, second)
        flushAndClear()

        val criteria = ProductFeedCriteria(sort = ProductFeedSort.TOGETHER_POPULAR)
        val firstPage = productRepository.findFeed(criteria, cursor = null, limit = 1)
        val secondPage = productRepository.findFeed(
            criteria = criteria,
            cursor = ProductFeedCursor.TogetherPopular(activeRoomCount = 2, id = popular.id),
            limit = 2,
        )

        assertThat(firstPage.map { it.id }).containsExactly(popular.id)
        assertThat(secondPage.map { it.id }).contains(second.id)
        assertThat(secondPage.map { it.id }).doesNotContain(popular.id)
    }

    private fun persistStore(
        name: String,
        region: RegionType,
        district: DistrictType,
        salesStatus: StoreSalesStatus = StoreSalesStatus.ACTIVE,
    ): Store {
        val store = Store(
            name = name,
            address = "테스트 주소",
            region = region,
            district = district,
            salesStatus = salesStatus,
        )
        em.persist(store)
        return store
    }

    private fun persistProduct(
        store: Store,
        name: String,
        categoryCode: String = Product.DEFAULT_CATEGORY_CODE,
        unitPrice: Int = 3_000,
        minQuantity: Int = 10,
        preparationDays: Int = 2,
        status: ProductStatus = ProductStatus.ACTIVE,
    ): Product {
        val product = Product(
            store = store,
            categoryCode = categoryCode,
            name = name,
            description = "$name 설명",
            unitPrice = unitPrice,
            minQuantity = minQuantity,
            maxQuantity = 100,
            preparationDays = preparationDays,
            storageType = ProductStorageType.ROOM_TEMPERATURE,
            status = status,
            thumbnailKey = "products/thumbnail.jpg",
        )
        em.persist(product)
        return product
    }

    private fun persistGroupBuy(store: Store, product: Product) {
        em.persist(
            GroupBuy(
                store = store,
                product = product,
                productName = product.name,
                productDescription = product.description,
                price = product.unitPrice,
                targetQuantity = product.minQuantity,
                currentQuantity = product.minQuantity,
                maxQuantity = product.maxQuantity,
                status = GroupBuyStatus.IN_PROGRESS,
                recruitmentStartAt = LocalDateTime.of(2026, 10, 26, 0, 0),
                deadline = LocalDateTime.of(2026, 10, 27, 23, 59),
                pickupDate = LocalDate.of(2026, 10, 30),
                pickupTimeStart = LocalTime.of(10, 0),
                pickupTimeEnd = LocalTime.of(11, 0),
                pickupLocation = store.address,
            )
        )
    }

    private fun flushAndClear() {
        em.flush()
        em.clear()
    }
}
