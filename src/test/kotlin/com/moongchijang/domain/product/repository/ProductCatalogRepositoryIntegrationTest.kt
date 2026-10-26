package com.moongchijang.domain.product.repository

import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductImage
import com.moongchijang.domain.product.domain.entity.ProductStatus
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
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductCatalogRepositoryIntegrationTest {

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var productImageRepository: ProductImageRepository

    @Autowired
    private lateinit var storeHolidayRepository: StoreHolidayRepository

    @Autowired
    private lateinit var storePickupTimeSlotRepository: StorePickupTimeSlotRepository

    @Autowired
    private lateinit var em: EntityManager

    @Test
    fun `상품 이미지는 상품별 노출 순서대로 조회한다`() {
        val product = persistProduct(persistStore("이미지 테스트 매장"), "이미지 테스트 상품")
        em.persist(ProductImage(product = product, imageKey = "products/third.jpg", sortOrder = 3))
        em.persist(ProductImage(product = product, imageKey = "products/first.jpg", sortOrder = 1))
        em.persist(ProductImage(product = product, imageKey = "products/second.jpg", sortOrder = 2))
        flushAndClear()

        val images = productImageRepository.findAllByProductIdOrderBySortOrderAscIdAsc(product.id)

        assertThat(images.map { it.imageKey }).containsExactly(
            "products/first.jpg",
            "products/second.jpg",
            "products/third.jpg",
        )
    }

    @Test
    fun `판매 가능한 상품은 활성 매장의 활성 상품만 조회한다`() {
        val activeStore = persistStore("판매 중 매장")
        val pausedStore = persistStore("판매 중지 매장", StoreSalesStatus.PAUSED)
        val saleableProduct = persistProduct(activeStore, "판매 상품")
        persistProduct(activeStore, "비활성 상품", ProductStatus.INACTIVE)
        persistProduct(pausedStore, "중지 매장 상품")
        flushAndClear()

        val products = productRepository.findAllSaleable(
            productStatus = ProductStatus.ACTIVE,
            storeSalesStatus = StoreSalesStatus.ACTIVE,
        )

        assertThat(products.map { it.id }).containsExactly(saleableProduct.id)
        assertThat(products.single().store.name).isEqualTo("판매 중 매장")
    }

    @Test
    fun `상품 카테고리 코드는 공백 제거와 대문자 정규화를 적용한다`() {
        val product = Product(
            store = persistStore("카테고리 테스트 매장"),
            categoryCode = " fresh_food ",
            name = "카테고리 테스트 상품",
            description = "상품 설명",
            unitPrice = 3_000,
            minQuantity = 10,
            maxQuantity = 100,
            preparationDays = 2,
            storageType = ProductStorageType.ROOM_TEMPERATURE,
            thumbnailKey = "products/thumbnail.jpg",
        )
        em.persist(product)
        flushAndClear()

        val saved = productRepository.findById(product.id).orElseThrow()

        assertThat(saved.categoryCode).isEqualTo("FRESH_FOOD")
    }

    @Test
    fun `활성 휴무일과 요일별 픽업 시간대를 조회한다`() {
        val store = persistStore("픽업 정책 테스트 매장")
        em.persist(
            StoreHoliday(
                store = store,
                holidayType = StoreHolidayType.REGULAR,
                dayOfWeek = 1,
                reason = "월요일 정기 휴무",
            )
        )
        em.persist(
            StoreHoliday(
                store = store,
                holidayType = StoreHolidayType.TEMPORARY,
                holidayDate = LocalDate.of(2026, 10, 30),
                reason = "임시 휴무",
                active = false,
            )
        )
        em.persist(
            StorePickupTimeSlot(
                store = store,
                dayOfWeek = 2,
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 0),
            )
        )
        em.persist(
            StorePickupTimeSlot(
                store = store,
                dayOfWeek = 2,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
            )
        )
        flushAndClear()

        val holidays = storeHolidayRepository.findAllByStoreIdAndActiveTrue(store.id)
        val timeSlots = storePickupTimeSlotRepository
            .findAllByStoreIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(store.id, 2)

        assertThat(holidays.map { it.reason }).containsExactly("월요일 정기 휴무")
        assertThat(timeSlots.map { it.startTime }).containsExactly(
            LocalTime.of(10, 0),
            LocalTime.of(14, 0),
        )
    }

    private fun persistStore(
        name: String,
        salesStatus: StoreSalesStatus = StoreSalesStatus.ACTIVE,
    ): Store {
        val store = Store(
            name = name,
            address = "서울 강남구 테헤란로",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
            salesStatus = salesStatus,
        )
        em.persist(store)
        return store
    }

    private fun persistProduct(
        store: Store,
        name: String,
        status: ProductStatus = ProductStatus.ACTIVE,
    ): Product {
        val product = Product(
            store = store,
            name = name,
            description = "$name 설명",
            unitPrice = 3_000,
            minQuantity = 10,
            maxQuantity = 100,
            preparationDays = 2,
            storageType = ProductStorageType.ROOM_TEMPERATURE,
            status = status,
            thumbnailKey = "products/thumbnail.jpg",
        )
        em.persist(product)
        return product
    }

    private fun flushAndClear() {
        em.flush()
        em.clear()
    }
}
