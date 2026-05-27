package com.moongchijang.domain.user.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.application.dto.BuyerWithdrawalBlockingReason
import com.moongchijang.domain.user.application.dto.BuyerWithdrawalContext
import com.moongchijang.domain.user.application.dto.SellerWithdrawalBlockingReason
import com.moongchijang.domain.user.application.dto.SellerWithdrawalContext
import com.moongchijang.domain.user.application.dto.WithdrawalContextResponse
import com.moongchijang.domain.user.application.dto.WithdrawalScreenType
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WithdrawalContextService(
    private val userRepository: UserRepository,
    private val participationRepository: ParticipationRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val groupBuyRepository: GroupBuyRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getContext(userId: Long): WithdrawalContextResponse {
        log.info("[WithdrawalContextService] 탈퇴 컨텍스트 조회 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        val buyer = resolveBuyerContext(userId)
        val seller = resolveSellerContext(userId, user.hasRole(UserRole.SELLER), buyer.hasPendingPickup)

        val recommended = decideRecommendedScreen(
            currentRole = user.role,
            buyerCanProceed = buyer.canProceed,
            sellerCanProceed = seller.canProceed,
        )
        val currentScreen = if (user.role == UserRole.SELLER) {
            WithdrawalScreenType.SELLER_WITHDRAWAL
        } else {
            WithdrawalScreenType.BUYER_WITHDRAWAL
        }
        val forceRedirect = recommended != currentScreen

        val response = WithdrawalContextResponse(
            currentRole = user.role,
            buyer = buyer,
            seller = seller,
            recommendedScreen = recommended,
            forceRedirect = forceRedirect,
            forceRedirectTarget = if (forceRedirect) recommended else null,
        )
        log.info(
            "[WithdrawalContextService] 탈퇴 컨텍스트 조회 완료: userId={}, recommendedScreen={}, forceRedirect={}",
            userId, response.recommendedScreen, response.forceRedirect
        )
        return response
    }

    private fun resolveBuyerContext(userId: Long): BuyerWithdrawalContext {
        val hasPendingPickup = participationRepository.existsPendingPickupForWithdrawal(
            userId = userId,
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
            groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
        )
        val hasActiveParticipation = participationRepository.existsByUserIdAndStatus(
            userId = userId,
            status = ParticipationStatus.PAID_WAITING_GOAL,
        )
        return BuyerWithdrawalContext(
            canProceed = !hasPendingPickup,
            hasPendingPickup = hasPendingPickup,
            hasActiveParticipation = hasActiveParticipation,
            blockingReason = if (hasPendingPickup) BuyerWithdrawalBlockingReason.PENDING_PICKUP else BuyerWithdrawalBlockingReason.NONE,
        )
    }

    private fun resolveSellerContext(
        userId: Long,
        hasSellerRole: Boolean,
        hasPendingPickupAsBuyer: Boolean,
    ): SellerWithdrawalContext {
        if (!hasSellerRole) {
            return SellerWithdrawalContext(
                canProceed = false,
                hasOpenGroupBuy = false,
                hasPendingCustomerPickup = false,
                blockingReason = SellerWithdrawalBlockingReason.NONE,
            )
        }

        val storeIds = storeStaffRepository.findStoreIdsByUserId(userId)
        if (storeIds.isEmpty()) {
            return SellerWithdrawalContext(
                canProceed = !hasPendingPickupAsBuyer,
                hasOpenGroupBuy = false,
                hasPendingCustomerPickup = false,
                blockingReason = if (hasPendingPickupAsBuyer) SellerWithdrawalBlockingReason.PENDING_CUSTOMER_PICKUP else SellerWithdrawalBlockingReason.NONE,
            )
        }

        val hasOpenGroupBuy = groupBuyRepository.existsByStoreIdInAndStatusIn(
            storeIds = storeIds,
            statuses = listOf(GroupBuyStatus.IN_PROGRESS),
        )
        val hasPendingCustomerPickup = participationRepository.existsUnpickedParticipationByStoreIdsAndGroupBuyStatuses(
            storeIds = storeIds,
            groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
            participationStatuses = listOf(ParticipationStatus.CONFIRMED),
        )
        val blockingReason = when {
            hasOpenGroupBuy -> SellerWithdrawalBlockingReason.OPEN_GROUPBUY
            hasPendingCustomerPickup || hasPendingPickupAsBuyer -> SellerWithdrawalBlockingReason.PENDING_CUSTOMER_PICKUP
            else -> SellerWithdrawalBlockingReason.NONE
        }

        return SellerWithdrawalContext(
            canProceed = !hasOpenGroupBuy && !hasPendingCustomerPickup && !hasPendingPickupAsBuyer,
            hasOpenGroupBuy = hasOpenGroupBuy,
            hasPendingCustomerPickup = hasPendingCustomerPickup,
            blockingReason = blockingReason,
        )
    }

    private fun decideRecommendedScreen(
        currentRole: UserRole,
        buyerCanProceed: Boolean,
        sellerCanProceed: Boolean,
    ): WithdrawalScreenType {
        return when {
            buyerCanProceed && !sellerCanProceed -> WithdrawalScreenType.BUYER_WITHDRAWAL
            !buyerCanProceed && sellerCanProceed -> WithdrawalScreenType.SELLER_WITHDRAWAL
            currentRole == UserRole.SELLER -> WithdrawalScreenType.SELLER_WITHDRAWAL
            else -> WithdrawalScreenType.BUYER_WITHDRAWAL
        }
    }
}
