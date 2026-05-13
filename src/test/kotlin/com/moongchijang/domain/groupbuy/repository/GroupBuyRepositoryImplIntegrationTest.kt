package com.moongchijang.domain.groupbuy.repository

import com.moongchijang.domain.favorite.domain.entity.Favorite
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.FeedSortMode
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupBuyRepositoryImplIntegrationTest {
    private val pageable = PageRequest.of(0, 10)

    @Autowired
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Autowired
    private lateinit var em: EntityManager

    @Test
    fun `전국 fallback 정렬 우선순위 검증`() {
        val baseStore = persistStore("기준 매장")
        val request = persistGroupBuyRequest()

        val gbMostFavorite = persistGroupBuy(
            store = baseStore,
            request = request,
            productName = "A",
            currentQuantity = 20,
            targetQuantity = 100,
            deadline = LocalDateTime.now().plusDays(5)
        )
        val gbFavoriteTieHighAchieve = persistGroupBuy(
            store = baseStore,
            request = request,
            productName = "B",
            currentQuantity = 90,
            targetQuantity = 100,
            deadline = LocalDateTime.now().plusDays(4)
        )
        val gbFavoriteTieLowAchieve = persistGroupBuy(
            store = baseStore,
            request = request,
            productName = "C",
            currentQuantity = 70,
            targetQuantity = 100,
            deadline = LocalDateTime.now().plusDays(3)
        )

        repeat(3) { persistFavorite(gbMostFavorite) }
        repeat(1) { persistFavorite(gbFavoriteTieHighAchieve) }
        repeat(1) { persistFavorite(gbFavoriteTieLowAchieve) }

        flushAndClear()
        val result = searchFeed(
            districtFilters = emptySet(),
            sortMode = FeedSortMode.NATIONWIDE_FALLBACK
        )

        assertOrder(
            result.content.map { it.id },
            gbMostFavorite.id,
            gbFavoriteTieHighAchieve.id,
            gbFavoriteTieLowAchieve.id
        )
    }

    @Test
    fun `지역 정렬 우선순위 검증`() {
        val seoulStore = persistStore("서울 매장", district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG)
        val request = persistGroupBuyRequest()

        val highAchievement = persistGroupBuy(
            store = seoulStore,
            request = request,
            productName = "고달성",
            currentQuantity = 95,
            targetQuantity = 100,
            deadline = LocalDateTime.now().plusDays(10)
        )
        val sameAchievementEarlyDeadline = persistGroupBuy(
            store = seoulStore,
            request = request,
            productName = "동률-빠른마감",
            currentQuantity = 80,
            targetQuantity = 100,
            deadline = LocalDateTime.now().plusDays(2)
        )
        val sameAchievementLateDeadline = persistGroupBuy(
            store = seoulStore,
            request = request,
            productName = "동률-늦은마감",
            currentQuantity = 80,
            targetQuantity = 100,
            deadline = LocalDateTime.now().plusDays(7)
        )

        flushAndClear()
        val result = searchFeed(
            districtFilters = setOf(DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG),
            sortMode = FeedSortMode.REGIONAL
        )

        assertOrder(
            result.content.map { it.id },
            highAchievement.id,
            sameAchievementEarlyDeadline.id,
            sameAchievementLateDeadline.id
        )
    }

    private fun searchFeed(
        districtFilters: Set<DistrictType>,
        sortMode: FeedSortMode
    ): org.springframework.data.domain.Page<GroupBuy> {
        return groupBuyRepository.searchFeed(
            filter = GroupBuyFeedFilter.ALL,
            districtFilters = districtFilters,
            pageable = pageable,
            sortMode = sortMode
        )
    }

    private fun assertOrder(actual: List<Long>, vararg expected: Long) {
        assertThat(actual).containsExactly(*expected.toTypedArray())
    }

    private fun flushAndClear() {
        em.flush()
        em.clear()
    }

    private fun persistUser(email: String): User {
        val user = User(
            provider = AuthProvider.EMAIL,
            email = email,
            passwordHash = "pw",
            nickname = "tester",
            role = UserRole.BUYER,
            signupCompleted = true
        )
        em.persist(user)
        return user
    }

    private fun persistStore(
        name: String,
        district: DistrictType = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG
    ): Store {
        val store = Store(
            name = name,
            address = "서울 강남구",
            region = RegionType.SEOUL,
            district = district
        )
        em.persist(store)
        return store
    }

    private fun persistGroupBuyRequest(): GroupBuyRequest {
        val request = GroupBuyRequest(
            userId = 1L,
            storeName = "테스트 매장",
            storeAddress = "서울 강남구",
            productName = "테스트 상품",
            desiredQuantity = 50,
            desiredPickupDate = LocalDate.now().plusDays(3)
        )
        em.persist(request)
        return request
    }

    private fun persistGroupBuy(
        store: Store,
        request: GroupBuyRequest,
        productName: String,
        currentQuantity: Int,
        targetQuantity: Int,
        deadline: LocalDateTime
    ): GroupBuy {
        val groupBuy = GroupBuy(
            store = store,
            groupBuyRequest = request,
            thumbnailUrl = "https://example.com/$productName.jpg",
            productName = productName,
            productDescription = "설명",
            price = 10000,
            targetQuantity = targetQuantity,
            currentQuantity = currentQuantity,
            maxQuantity = 200,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = deadline,
            pickupDate = LocalDate.now().plusDays(5),
            pickupTimeStart = LocalTime.of(10, 0),
            pickupTimeEnd = LocalTime.of(12, 0),
            pickupLocation = "서울 강남구",
            shareCount = 0
        )
        em.persist(groupBuy)
        return groupBuy
    }

    private fun persistFavorite(groupBuy: GroupBuy) {
        val user = persistUser("user-${System.nanoTime()}@test.com")
        em.persist(
            Favorite(
                user = user,
                groupBuy = groupBuy
            )
        )
    }
}
