package com.moongchijang.domain.favorite.application

import com.moongchijang.domain.favorite.domain.entity.Favorite
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class WishlistCommandServiceTest {

    @Mock
    private lateinit var favoriteRepository: FavoriteRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private val service by lazy {
        WishlistCommandService(
            favoriteRepository = favoriteRepository,
            groupBuyRepository = groupBuyRepository,
            userRepository = userRepository,
        )
    }

    @Test
    fun `찜 추가 요청 시 신규 찜 저장`() {
        val userId = 1L
        val groupBuyId = 10L
        val user = UserFixture.createKakaoUser(id = userId)
        val groupBuy = GroupBuyFixture.createGroupBuy(id = groupBuyId, status = GroupBuyStatus.IN_PROGRESS)

        `when`(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(user)
        `when`(groupBuyRepository.findById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)

        service.addWishlist(userId, groupBuyId)

        verify(favoriteRepository, times(1)).saveAndFlush(any(Favorite::class.java))
    }

    @Test
    fun `찜 추가 요청 시 기존 찜 멱등 성공`() {
        val userId = 2L
        val groupBuyId = 20L
        val user = UserFixture.createKakaoUser(id = userId)
        val groupBuy = GroupBuyFixture.createGroupBuy(id = groupBuyId, status = GroupBuyStatus.IN_PROGRESS)

        `when`(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(user)
        `when`(groupBuyRepository.findById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(true)

        assertDoesNotThrow { service.addWishlist(userId, groupBuyId) }
        verify(favoriteRepository, never()).saveAndFlush(any(Favorite::class.java))
    }

    @Test
    fun `찜 추가 요청 시 동시성 충돌 멱등 성공`() {
        val userId = 3L
        val groupBuyId = 30L
        val user = UserFixture.createKakaoUser(id = userId)
        val groupBuy = GroupBuyFixture.createGroupBuy(id = groupBuyId, status = GroupBuyStatus.IN_PROGRESS)

        `when`(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(user)
        `when`(groupBuyRepository.findById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)
        `when`(favoriteRepository.saveAndFlush(any(Favorite::class.java))).thenThrow(DataIntegrityViolationException("dup"))

        assertDoesNotThrow { service.addWishlist(userId, groupBuyId) }
    }

    @Test
    fun `찜 해제 요청 시 찜 미존재 멱등 성공`() {
        val userId = 4L
        val groupBuyId = 40L
        `when`(groupBuyRepository.existsById(groupBuyId)).thenReturn(true)
        `when`(favoriteRepository.deleteByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(0)

        assertDoesNotThrow { service.removeWishlist(userId, groupBuyId) }
        verify(favoriteRepository).deleteByUserIdAndGroupBuyId(userId, groupBuyId)
    }

    @Test
    fun `찜 해제 요청 시 공구 미존재 예외 반환`() {
        val userId = 5L
        val groupBuyId = 50L
        `when`(groupBuyRepository.existsById(groupBuyId)).thenReturn(false)

        val ex = assertThrows<CustomException> {
            service.removeWishlist(userId, groupBuyId)
        }

        assertEquals(ErrorCode.GROUPBUY_NOT_FOUND, ex.errorCode)
    }
}
