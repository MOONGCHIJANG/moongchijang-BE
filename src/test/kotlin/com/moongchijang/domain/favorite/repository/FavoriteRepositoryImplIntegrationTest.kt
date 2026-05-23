package com.moongchijang.domain.favorite.repository

import com.moongchijang.domain.favorite.application.dto.WishFilterType
import com.moongchijang.domain.favorite.application.dto.WishSortType
import com.moongchijang.domain.favorite.domain.entity.Favorite
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FavoriteRepositoryImplIntegrationTest {

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var em: EntityManager

    @Test
    fun `찜 목록 조회 시 groupBuy와 store를 함께 조회한다`() {
        val user = persistUser("favorite-user@test.com")
        val groupBuy = persistGroupBuy()
        em.persist(Favorite(user = user, groupBuy = groupBuy))
        flushAndClear()

        val page = favoriteRepository.findWishlistGroupBuys(
            userId = user.id!!,
            filter = WishFilterType.ALL,
            excludeClosed = false,
            sort = WishSortType.LATEST,
            pageable = PageRequest.of(0, 20),
            now = LocalDateTime.of(2026, 5, 23, 10, 0),
        )

        assertThat(page.content).hasSize(1)
        assertThat(page.content.first().store.name).isEqualTo("찜 테스트 매장")
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
            signupCompleted = true,
        )
        em.persist(user)
        return user
    }

    private fun persistGroupBuy(): GroupBuy {
        val store = Store(
            name = "찜 테스트 매장",
            address = "서울 강남구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        )
        em.persist(store)

        val request = GroupBuyRequest(
            userId = 1L,
            storeName = "찜 테스트 매장",
            storeAddress = "서울 강남구",
            productName = "테스트 상품",
            desiredQuantity = 50,
            desiredPickupDate = LocalDate.now().plusDays(3),
        )
        em.persist(request)

        val groupBuy = GroupBuy(
            store = store,
            groupBuyRequest = request,
            thumbnailUrl = "https://example.com/test.jpg",
            productName = "테스트 상품",
            productDescription = "설명",
            price = 10000,
            targetQuantity = 50,
            currentQuantity = 10,
            maxQuantity = 100,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = LocalDateTime.of(2026, 5, 30, 21, 0),
            pickupDate = LocalDate.of(2026, 6, 1),
            pickupTimeStart = LocalTime.of(10, 0),
            pickupTimeEnd = LocalTime.of(12, 0),
            pickupLocation = "서울 강남구",
            shareCount = 0,
        )
        em.persist(groupBuy)
        return groupBuy
    }
}
