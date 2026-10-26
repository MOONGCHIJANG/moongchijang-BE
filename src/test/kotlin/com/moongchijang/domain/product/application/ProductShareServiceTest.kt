package com.moongchijang.domain.product.application

import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ProductShareServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var s3ImageReferenceResolver: S3ImageReferenceResolver

    private lateinit var service: ProductShareService

    @BeforeEach
    fun setUp() {
        service = ProductShareService(
            productRepository = productRepository,
            s3ImageReferenceResolver = s3ImageReferenceResolver,
            shareBaseUrl = "https://moongchijang.com/",
        )
    }

    @Test
    fun `상품 공유 메타데이터를 반환한다`() {
        val product = createProduct()
        `when`(productRepository.findWithStoreById(product.id)).thenReturn(product)
        `when`(s3ImageReferenceResolver.resolveForRead(product.thumbnailKey))
            .thenReturn("https://cdn.moongchijang.com/products/thumbnail.jpg")

        val result = service.getShareMeta(product.id)

        assertThat(result.productName).isEqualTo("공유 테스트 상품")
        assertThat(result.storeName).isEqualTo("공유 테스트 매장")
        assertThat(result.imageUrl).isEqualTo("https://cdn.moongchijang.com/products/thumbnail.jpg")
        assertThat(result.shareUrl).isEqualTo("https://moongchijang.com/products/20")
    }

    @Test
    fun `공유할 상품이 없으면 예외를 발생시킨다`() {
        `when`(productRepository.findWithStoreById(999L)).thenReturn(null)

        val exception = assertThrows<CustomException> { service.getShareMeta(999L) }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
    }

    private fun createProduct(): Product {
        val store = Store(
            name = "공유 테스트 매장",
            address = "서울 강남구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
            id = 10L,
        )
        return Product(
            store = store,
            name = "공유 테스트 상품",
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
}
