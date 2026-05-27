package com.moongchijang.domain.user.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.application.dto.BuyerWithdrawalBlockingReason
import com.moongchijang.domain.user.application.dto.SellerWithdrawalBlockingReason
import com.moongchijang.domain.user.application.dto.WithdrawalScreenType
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.entity.UserRoleAssignment
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class WithdrawalContextServiceTest {

    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val participationRepository: ParticipationRepository = Mockito.mock(ParticipationRepository::class.java)
    private val storeStaffRepository: StoreStaffRepository = Mockito.mock(StoreStaffRepository::class.java)
    private val groupBuyRepository: GroupBuyRepository = Mockito.mock(GroupBuyRepository::class.java)

    private val withdrawalContextService = WithdrawalContextService(
        userRepository = userRepository,
        participationRepository = participationRepository,
        storeStaffRepository = storeStaffRepository,
        groupBuyRepository = groupBuyRepository,
    )

    @Test
    fun `탈퇴 컨텍스트 소비자 기본 진입 화면 반환`() {
        val user = UserFixture.createKakaoUser(id = 101L, providerId = "ctx-101").apply {
            role = UserRole.BUYER
        }
        stubBuyerContext(
            user = user,
            hasPendingPickup = false,
            hasActiveParticipation = true,
        )

        val result = withdrawalContextService.getContext(101L)

        assertEquals(WithdrawalScreenType.BUYER_WITHDRAWAL, result.recommendedScreen)
        assertFalse(result.forceRedirect)
        assertTrue(result.buyer.canProceed)
        assertEquals(BuyerWithdrawalBlockingReason.NONE, result.buyer.blockingReason)
        assertFalse(result.seller.canProceed)
    }

    @Test
    fun `탈퇴 컨텍스트 사장님 역할일 때 사장님 화면 권장`() {
        val user = sellerUser(id = 102L)
        stubBuyerContext(
            user = user,
            hasPendingPickup = false,
            hasActiveParticipation = false,
        )
        Mockito.`when`(storeStaffRepository.findStoreIdsByUserId(102L)).thenReturn(emptyList())

        val result = withdrawalContextService.getContext(102L)

        assertEquals(WithdrawalScreenType.SELLER_WITHDRAWAL, result.recommendedScreen)
        assertFalse(result.forceRedirect)
        assertTrue(result.buyer.canProceed)
        assertTrue(result.seller.canProceed)
    }

    @Test
    fun `탈퇴 컨텍스트 소비자 수령예정 공구 차단`() {
        val user = UserFixture.createKakaoUser(id = 103L, providerId = "ctx-103")
        stubBuyerContext(
            user = user,
            hasPendingPickup = true,
            hasActiveParticipation = false,
        )

        val result = withdrawalContextService.getContext(103L)

        assertFalse(result.buyer.canProceed)
        assertEquals(BuyerWithdrawalBlockingReason.PENDING_PICKUP, result.buyer.blockingReason)
    }

    @Test
    fun `탈퇴 컨텍스트 사장님 진행중 공구 차단`() {
        val user = sellerUser(id = 104L)
        stubBuyerContext(
            user = user,
            hasPendingPickup = false,
            hasActiveParticipation = false,
        )
        Mockito.`when`(storeStaffRepository.findStoreIdsByUserId(104L)).thenReturn(listOf(9001L))
        Mockito.`when`(
            groupBuyRepository.existsByStoreIdInAndStatusIn(
                listOf(9001L),
                listOf(GroupBuyStatus.IN_PROGRESS),
            )
        ).thenReturn(true)
        Mockito.`when`(
            participationRepository.existsUnpickedParticipationByStoreIdsAndGroupBuyStatuses(
                storeIds = listOf(9001L),
                groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
            )
        ).thenReturn(false)

        val result = withdrawalContextService.getContext(104L)

        assertFalse(result.seller.canProceed)
        assertEquals(SellerWithdrawalBlockingReason.OPEN_GROUPBUY, result.seller.blockingReason)
    }

    @Test
    fun `탈퇴 컨텍스트 사장님 화면에서 소비자 화면 강제 이동`() {
        val user = sellerUser(id = 105L)
        stubBuyerContext(
            user = user,
            hasPendingPickup = false,
            hasActiveParticipation = false,
        )
        Mockito.`when`(storeStaffRepository.findStoreIdsByUserId(105L)).thenReturn(listOf(9002L))
        Mockito.`when`(
            groupBuyRepository.existsByStoreIdInAndStatusIn(
                listOf(9002L),
                listOf(GroupBuyStatus.IN_PROGRESS),
            )
        ).thenReturn(true)
        Mockito.`when`(
            participationRepository.existsUnpickedParticipationByStoreIdsAndGroupBuyStatuses(
                storeIds = listOf(9002L),
                groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
            )
        ).thenReturn(false)

        val result = withdrawalContextService.getContext(105L)

        assertEquals(WithdrawalScreenType.SELLER_WITHDRAWAL, result.recommendedScreen)
        assertFalse(result.forceRedirect)
        assertEquals(null, result.forceRedirectTarget)
        assertEquals(SellerWithdrawalBlockingReason.OPEN_GROUPBUY, result.seller.blockingReason)
    }

    @Test
    fun `탈퇴 컨텍스트 사장님 경로 진입 시 본인 수령예정은 소비자 화면 강제 이동`() {
        val user = sellerUser(id = 106L)
        stubBuyerContext(
            user = user,
            hasPendingPickup = true,
            hasActiveParticipation = false,
        )
        Mockito.`when`(storeStaffRepository.findStoreIdsByUserId(106L)).thenReturn(emptyList())

        val result = withdrawalContextService.getContext(106L)

        assertTrue(result.seller.canProceed)
        assertEquals(SellerWithdrawalBlockingReason.NONE, result.seller.blockingReason)
        assertEquals(WithdrawalScreenType.BUYER_WITHDRAWAL, result.recommendedScreen)
        assertTrue(result.forceRedirect)
        assertEquals(WithdrawalScreenType.BUYER_WITHDRAWAL, result.forceRedirectTarget)
    }

    private fun stubBuyerContext(
        user: com.moongchijang.domain.user.domain.entity.User,
        hasPendingPickup: Boolean,
        hasActiveParticipation: Boolean,
    ) {
        val userId = user.id!!
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(user)
        Mockito.`when`(
            participationRepository.existsPendingPickupForWithdrawal(
                userId = userId,
                participationStatus = ParticipationStatus.CONFIRMED,
                pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
            )
        ).thenReturn(hasPendingPickup)
        Mockito.`when`(
            participationRepository.existsByUserIdAndStatus(
                userId = userId,
                status = ParticipationStatus.PAID_WAITING_GOAL,
            )
        ).thenReturn(hasActiveParticipation)
    }

    private fun sellerUser(id: Long): com.moongchijang.domain.user.domain.entity.User {
        return UserFixture.createKakaoUser(id = id, providerId = "ctx-$id").apply {
            role = UserRole.SELLER
            roleAssignments.add(UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
    }
}
