package com.moongchijang.domain.favorite.application

import com.moongchijang.domain.favorite.application.dto.WishFilterType
import com.moongchijang.domain.favorite.application.dto.WishSortType
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.global.util.S3ImageReferenceResolver
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class WishlistQueryServiceTest {

    @Mock
    private lateinit var favoriteRepository: FavoriteRepository

    @Mock
    private lateinit var s3ImageReferenceResolver: S3ImageReferenceResolver

    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-22T01:00:00Z"), ZoneOffset.UTC)

    private val service by lazy { WishlistQueryService(favoriteRepository, s3ImageReferenceResolver, clock) }

    @Test
    fun `찜 목록 조회 요청 시 페이지 결과 반환`() {
        val userId = 1L
        val pageable = PageRequest.of(0, 20)
        val now = LocalDateTime.of(2026, 5, 22, 10, 0)
        val item = createGroupBuy(
            id = 101L,
            deadline = now.plusDays(2),
            currentQuantity = 36,
            targetQuantity = 50,
        )

        `when`(
            favoriteRepository.findWishlistGroupBuys(
                userId = userId,
                filter = WishFilterType.ALL,
                excludeClosed = false,
                sort = WishSortType.LATEST,
                pageable = pageable,
                now = now,
            )
        ).thenReturn(PageImpl(listOf(item), pageable, 1))
        `when`(favoriteRepository.countUrgentByUserId(userId, now, now.plusHours(24))).thenReturn(2L)

        val result = service.getWishlist(
            userId = userId,
            filter = WishFilterType.ALL,
            excludeClosed = false,
            sort = WishSortType.LATEST,
            pageable = pageable,
            now = now,
        )

        verify(favoriteRepository).findWishlistGroupBuys(userId, WishFilterType.ALL, false, WishSortType.LATEST, pageable, now)
        verify(favoriteRepository).countUrgentByUserId(userId, now, now.plusHours(24))
        assertEquals(1, result.content.size)
        assertEquals(2, result.urgentCount)
    }

    @Test
    fun `찜 목록 조회 요청 시 카드 매핑 정보 반환`() {
        val userId = 2L
        val pageable = PageRequest.of(0, 10)
        val now = LocalDateTime.of(2026, 5, 22, 10, 0)
        val deadline = LocalDateTime.of(2026, 5, 24, 21, 0)
        val pickupDate = LocalDate.of(2026, 5, 25)
        val item = createGroupBuy(
            id = 202L,
            deadline = deadline,
            pickupDate = pickupDate,
            currentQuantity = 36,
            targetQuantity = 50,
            price = 18000,
        )

        `when`(
            favoriteRepository.findWishlistGroupBuys(
                userId = userId,
                filter = WishFilterType.CLOSING_SOON,
                excludeClosed = false,
                sort = WishSortType.DEADLINE,
                pageable = pageable,
                now = now,
            )
        ).thenReturn(PageImpl(listOf(item), pageable, 1))
        `when`(favoriteRepository.countUrgentByUserId(userId, now, now.plusHours(24))).thenReturn(0L)

        val result = service.getWishlist(userId, WishFilterType.CLOSING_SOON, false, WishSortType.DEADLINE, pageable, now)
        val card = result.content.first()

        assertEquals(202L, card.groupBuyId)
        assertEquals(2, card.dDay)
        assertEquals("D-2", card.dDayLabel)
        assertEquals("뭉치장 베이커리", card.storeName)
        assertEquals("서울", card.regionLabel)
        assertEquals("두쫀쿠 1개", card.productName)
        assertEquals(pickupDate, card.pickupDate)
        assertEquals("5/25(월)", card.pickupDateLabel)
        assertEquals(deadline, card.deadline)
        assertEquals("5/24(일) 21:00", card.deadlineLabel)
        assertEquals(72, card.achievementRate)
        assertEquals(18000, card.price)
        assertTrue(card.isWishlisted)
    }

    @Test
    fun `찜 목록 조회 시 D-day가 0이면 라벨 D-day 반환`() {
        val userId = 3L
        val pageable = PageRequest.of(0, 10)
        val now = LocalDateTime.of(2026, 5, 22, 10, 0)
        val deadline = LocalDateTime.of(2026, 5, 22, 21, 0)
        val item = createGroupBuy(id = 303L, deadline = deadline)

        `when`(
            favoriteRepository.findWishlistGroupBuys(
                userId = userId,
                filter = WishFilterType.ALL,
                excludeClosed = false,
                sort = WishSortType.LATEST,
                pageable = pageable,
                now = now,
            )
        ).thenReturn(PageImpl(listOf(item), pageable, 1))
        `when`(favoriteRepository.countUrgentByUserId(userId, now, now.plusHours(24))).thenReturn(1L)

        val result = service.getWishlist(userId, WishFilterType.ALL, false, WishSortType.LATEST, pageable, now)
        val card = result.content.first()

        assertEquals(0, card.dDay)
        assertEquals("D-day", card.dDayLabel)
    }

    private fun createGroupBuy(
        id: Long,
        deadline: LocalDateTime = LocalDateTime.of(2026, 5, 24, 21, 0),
        pickupDate: LocalDate = LocalDate.of(2026, 5, 25),
        currentQuantity: Int = 36,
        targetQuantity: Int = 50,
        price: Int = 6000,
    ): GroupBuy {
        return GroupBuyFixture.createGroupBuy(
            id = id,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = deadline,
            price = price,
            currentQuantity = currentQuantity,
            targetQuantity = targetQuantity,
        ).apply {
            this.pickupDate = pickupDate
        }
    }
}
