package com.moongchijang.domain.favorite.application

import com.moongchijang.domain.favorite.domain.entity.Favorite
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ProductWishlistCommandServiceTest {

    @Mock
    private lateinit var favoriteRepository: FavoriteRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private val service by lazy {
        ProductWishlistCommandService(
            favoriteRepository = favoriteRepository,
            productRepository = productRepository,
            userRepository = userRepository,
        )
    }

    @Test
    fun `상품 찜을 추가한다`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val product = createProduct()
        `when`(userRepository.findByIdAndDeletedAtIsNull(user.id!!)).thenReturn(user)
        `when`(productRepository.findById(product.id)).thenReturn(Optional.of(product))
        `when`(favoriteRepository.existsByUserIdAndProductId(user.id!!, product.id)).thenReturn(false)
        `when`(favoriteRepository.saveAndFlush(any(Favorite::class.java)))
            .thenAnswer { it.arguments[0] as Favorite }

        service.addWishlist(user.id!!, product.id)

        val captor = ArgumentCaptor.forClass(Favorite::class.java)
        verify(favoriteRepository).saveAndFlush(captor.capture())
        assertThat(captor.value.product?.id).isEqualTo(product.id)
        assertThat(captor.value.groupBuy).isNull()
    }

    @Test
    fun `이미 찜한 상품 추가는 멱등 처리한다`() {
        val user = UserFixture.createKakaoUser(id = 2L)
        val product = createProduct()
        `when`(userRepository.findByIdAndDeletedAtIsNull(user.id!!)).thenReturn(user)
        `when`(productRepository.findById(product.id)).thenReturn(Optional.of(product))
        `when`(favoriteRepository.existsByUserIdAndProductId(user.id!!, product.id)).thenReturn(true)

        assertDoesNotThrow { service.addWishlist(user.id!!, product.id) }

        verify(favoriteRepository, never()).saveAndFlush(any(Favorite::class.java))
    }

    @Test
    fun `상품 찜 추가 경합은 멱등 처리한다`() {
        val user = UserFixture.createKakaoUser(id = 3L)
        val product = createProduct()
        `when`(userRepository.findByIdAndDeletedAtIsNull(user.id!!)).thenReturn(user)
        `when`(productRepository.findById(product.id)).thenReturn(Optional.of(product))
        `when`(favoriteRepository.existsByUserIdAndProductId(user.id!!, product.id)).thenReturn(false)
        `when`(favoriteRepository.saveAndFlush(any(Favorite::class.java)))
            .thenThrow(DataIntegrityViolationException("duplicate"))

        assertDoesNotThrow { service.addWishlist(user.id!!, product.id) }
    }

    @Test
    fun `상품 찜 해제는 멱등 처리한다`() {
        `when`(productRepository.existsById(20L)).thenReturn(true)
        `when`(favoriteRepository.deleteByUserIdAndProductId(1L, 20L)).thenReturn(0)

        assertDoesNotThrow { service.removeWishlist(1L, 20L) }

        verify(favoriteRepository).deleteByUserIdAndProductId(1L, 20L)
    }

    @Test
    fun `존재하지 않는 상품의 찜 해제는 예외를 발생시킨다`() {
        `when`(productRepository.existsById(999L)).thenReturn(false)

        val exception = assertThrows<CustomException> { service.removeWishlist(1L, 999L) }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
    }

    private fun createProduct(): Product {
        val store = Store(
            name = "상품 찜 테스트 매장",
            address = "서울 강남구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
            id = 10L,
        )
        return Product(
            store = store,
            name = "상품 찜 테스트 상품",
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
