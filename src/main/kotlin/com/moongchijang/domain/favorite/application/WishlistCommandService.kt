package com.moongchijang.domain.favorite.application

import com.moongchijang.domain.favorite.domain.entity.Favorite
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WishlistCommandService(
    private val favoriteRepository: FavoriteRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun addWishlist(userId: Long, groupBuyId: Long) {
        log.info("[WishlistCommandService] 찜 추가 시작: userId={}, groupBuyId={}", userId, groupBuyId)

        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val groupBuy = groupBuyRepository.findById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

        if (favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)) {
            log.info("[WishlistCommandService] 찜 추가 멱등 처리: userId={}, groupBuyId={}", userId, groupBuyId)
            return
        }

        try {
            favoriteRepository.saveAndFlush(
                Favorite(
                    user = user,
                    groupBuy = groupBuy,
                )
            )
        } catch (e: DataIntegrityViolationException) {
            log.info("[WishlistCommandService] 찜 추가 경합 멱등 처리: userId={}, groupBuyId={}", userId, groupBuyId)
            return
        }

        log.info("[WishlistCommandService] 찜 추가 완료: userId={}, groupBuyId={}", userId, groupBuyId)
    }

    @Transactional
    fun removeWishlist(userId: Long, groupBuyId: Long) {
        log.info("[WishlistCommandService] 찜 해제 시작: userId={}, groupBuyId={}", userId, groupBuyId)

        if (!groupBuyRepository.existsById(groupBuyId)) {
            throw CustomException(ErrorCode.GROUPBUY_NOT_FOUND)
        }

        val deletedCount = favoriteRepository.deleteByUserIdAndGroupBuyId(userId, groupBuyId)
        log.info(
            "[WishlistCommandService] 찜 해제 완료: userId={}, groupBuyId={}, deletedCount={}",
            userId, groupBuyId, deletedCount
        )
    }
}
