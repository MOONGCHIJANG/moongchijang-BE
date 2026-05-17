package com.moongchijang.support

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object ParticipationFixture {

    fun createUser(id: Long): User =
        User(
            provider = AuthProvider.EMAIL,
            providerId = null,
            email = "user$id@example.com",
            passwordHash = "hashed",
            nickname = "user$id",
            phoneNumber = "01000000000",
            role = UserRole.BUYER,
            signupCompleted = true,
            deletedAt = null,
            id = id,
        )

    fun createGroupBuy(
        id: Long,
        status: GroupBuyStatus,
        deadline: LocalDateTime = LocalDateTime.now().plusDays(1),
        currentQuantity: Int = 1,
        targetQuantity: Int = 10,
        price: Int = 5000
    ): GroupBuy {
        return GroupBuy(
            store = createStore(),
            groupBuyRequest = createGroupBuyRequest(),
            thumbnailUrl = null,
            productName = "두쫀쿠",
            productDescription = "설명",
            price = price,
            targetQuantity = targetQuantity,
            currentQuantity = currentQuantity,
            maxQuantity = 100,
            status = status,
            deadline = deadline,
            pickupDate = LocalDate.now().plusDays(2),
            pickupTimeStart = LocalTime.of(14, 0),
            pickupTimeEnd = LocalTime.of(16, 0),
            pickupLocation = "서울",
            shareCount = 0,
            id = id
        )
    }

    fun createStore(): Store =
        Store(
            name = "테스트 매장",
            address = "서울 성동구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN,
            id = 1L
        )

    fun createGroupBuyRequest(): GroupBuyRequest =
        GroupBuyRequest(
            userId = 1L,
            storeName = "테스트 매장",
            storeAddress = "서울 성동구",
            productName = "두쫀쿠",
            desiredQuantity = 10,
            desiredPickupDate = LocalDate.now().plusDays(3),
            id = 1L
        )
}
