package com.moongchijang.domain.participation.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.config.QuerydslConfig
import com.moongchijang.global.config.TimeConfig
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
@Import(QuerydslConfig::class, TimeConfig::class)
class ParticipationRepositoryIntegrationTest {

    @Autowired
    private lateinit var participationRepository: ParticipationRepository

    @Autowired
    private lateinit var em: EntityManager

    @Test
    fun `어드민 검토 대기 환불 건수는 환불 대기 중 사장님 검토 대기와 미검토만 집계한다`() {
        val groupBuy = persistGroupBuy()
        persistParticipation(groupBuy, "pending@example.com", ParticipationStatus.REFUND_PENDING, OwnerRefundReviewStatus.PENDING)
        persistParticipation(groupBuy, "null@example.com", ParticipationStatus.REFUND_PENDING, null)
        persistParticipation(groupBuy, "approved@example.com", ParticipationStatus.REFUND_PENDING, OwnerRefundReviewStatus.APPROVED)
        persistParticipation(groupBuy, "disputed@example.com", ParticipationStatus.REFUND_PENDING, OwnerRefundReviewStatus.DISPUTED)
        persistParticipation(groupBuy, "refunded@example.com", ParticipationStatus.REFUNDED, OwnerRefundReviewStatus.PENDING)
        em.flush()
        em.clear()

        val result = participationRepository.countPendingAdminRefundReviews(
            status = ParticipationStatus.REFUND_PENDING,
            pendingReviewStatus = OwnerRefundReviewStatus.PENDING
        )

        assertThat(result).isEqualTo(2L)
    }

    private fun persistGroupBuy(): GroupBuy {
        val requester = persistUser("requester@example.com")
        val store = Store(
            name = "테스트 매장",
            address = "서울 성동구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN
        )
        em.persist(store)

        val request = GroupBuyRequest(
            user = requester,
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
            recruitmentStartAt = LocalDateTime.now(),
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

    private fun persistParticipation(
        groupBuy: GroupBuy,
        userEmail: String,
        status: ParticipationStatus,
        ownerRefundReviewStatus: OwnerRefundReviewStatus?,
    ): Participation {
        val participation = Participation(
            user = persistUser(userEmail),
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 1000,
            feeAmount = 0,
            totalAmount = 1000,
            status = status,
            ownerRefundReviewStatus = ownerRefundReviewStatus
        )
        em.persist(participation)
        return participation
    }

    private fun persistUser(email: String): User {
        val user = User(
            provider = AuthProvider.EMAIL,
            email = email,
            passwordHash = "password",
            nickname = email.substringBefore("@"),
            role = UserRole.BUYER,
            signupCompleted = true
        )
        em.persist(user)
        return user
    }
}
