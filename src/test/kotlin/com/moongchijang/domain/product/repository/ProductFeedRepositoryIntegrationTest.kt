package com.moongchijang.domain.product.repository

import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.product.domain.repository.ProductFeedCriteria
import com.moongchijang.domain.product.domain.repository.ProductFeedCursor
import com.moongchijang.domain.product.domain.repository.ProductFeedSort
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

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

    private fun flushAndClear() {
        em.flush()
        em.clear()
    }
}
