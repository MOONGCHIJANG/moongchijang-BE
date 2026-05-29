package com.moongchijang.domain.participation.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.config.QuerydslConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DataJpaTest(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    ]
)
@Import(QuerydslConfig::class)
class ParticipationAuditingIntegrationTest {

    @Autowired
    private lateinit var em: EntityManager

    @Test
    fun `참여 저장 시 감사 시간이 채워진다`() {
        val user = persistUser()
        val groupBuy = persistGroupBuy(user.id!!)
        val participation = Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 1000,
            feeAmount = 0,
            totalAmount = 1000,
            status = ParticipationStatus.PAID_WAITING_GOAL
        )

        em.persist(participation)
        em.flush()

        assertThat(participation.createdAt).isNotNull()
        assertThat(participation.updatedAt).isNotNull()
    }

    private fun persistUser(): User {
        val user = User(
            provider = AuthProvider.EMAIL,
            email = "participation-audit@test.com",
            passwordHash = "password",
            nickname = "tester",
            role = UserRole.BUYER,
            signupCompleted = true
        )
        em.persist(user)
        return user
    }

    private fun persistGroupBuy(userId: Long): GroupBuy {
        val store = Store(
            name = "테스트 매장",
            address = "서울 성동구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN
        )
        em.persist(store)

        val request = GroupBuyRequest(
            userId = userId,
            storeName = "테스트 매장",
            storeAddress = "서울 성동구",
            productName = "테스트 상품",
            desiredQuantity = 50,
            desiredPickupDate = LocalDate.now().plusDays(5)
        )
        em.persist(request)

        val groupBuy = GroupBuy(
            store = store,
            groupBuyRequest = request,
            thumbnailKey = "https://example.com/image.jpg",
            productName = "테스트 상품",
            productDescription = "설명",
            price = 1000,
            targetQuantity = 50,
            currentQuantity = 0,
            maxQuantity = 100,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = LocalDateTime.now().plusDays(3),
            pickupDate = LocalDate.now().plusDays(5),
            pickupTimeStart = LocalTime.of(14, 0),
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "서울 성동구",
            shareCount = 0
        )
        em.persist(groupBuy)
        return groupBuy
    }
}
