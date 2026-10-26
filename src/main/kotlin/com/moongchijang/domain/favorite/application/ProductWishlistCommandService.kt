package com.moongchijang.domain.favorite.application

import com.moongchijang.domain.favorite.domain.entity.Favorite
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductWishlistCommandService(
    private val favoriteRepository: FavoriteRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun addWishlist(userId: Long, productId: Long) {
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val product = productRepository.findById(productId)
            .orElseThrow { CustomException(ErrorCode.PRODUCT_NOT_FOUND) }

        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            return
        }

        try {
            favoriteRepository.saveAndFlush(
                Favorite(
                    user = user,
                    product = product,
                )
            )
        } catch (e: DataIntegrityViolationException) {
            log.info("[ProductWishlistCommandService] 상품 찜 추가 경합 멱등 처리: userId={}, productId={}", userId, productId)
        }
    }

    @Transactional
    fun removeWishlist(userId: Long, productId: Long) {
        if (!productRepository.existsById(productId)) {
            throw CustomException(ErrorCode.PRODUCT_NOT_FOUND)
        }
        favoriteRepository.deleteByUserIdAndProductId(userId, productId)
    }
}
