package com.moongchijang.domain.user.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.application.dto.OwnerWithdrawRequest
import com.moongchijang.domain.user.domain.entity.OwnerWithdrawalReason
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OwnerWithdrawService(
    private val userRepository: UserRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun withdraw(ownerId: Long, request: OwnerWithdrawRequest) {
        log.info("[OwnerWithdrawService] 사장님 회원탈퇴 처리 시작: ownerId={}", ownerId)
        val owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (!owner.hasRole(UserRole.SELLER)) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        validateReason(request)
        validateWithdrawable(ownerId)

        owner.withdrawAsOwner(
            reason = request.reason,
            reasonDetail = normalizedReasonDetail(request),
        )
        userRepository.save(owner)
        log.info("[OwnerWithdrawService] 사장님 회원탈퇴 처리 완료: ownerId={}", ownerId)
    }

    fun validateWithdrawable(ownerId: Long) {
        val hasPendingPickupAsConsumer = participationRepository.existsPendingPickupForWithdrawal(
            userId = ownerId,
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
            groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
        )
        if (hasPendingPickupAsConsumer) {
            throw CustomException(ErrorCode.WITHDRAWAL_BLOCKED_PENDING_PICKUP)
        }

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            return
        }

        val hasOpenGroupBuy = groupBuyRepository.existsByStoreIdInAndStatusIn(
            storeIds = storeIds,
            statuses = listOf(GroupBuyStatus.IN_PROGRESS),
        )
        if (hasOpenGroupBuy) {
            throw CustomException(ErrorCode.OWNER_WITHDRAWAL_BLOCKED_OPEN_GROUPBUY)
        }

        val hasPendingCustomerPickup = participationRepository.existsUnpickedParticipationByStoreIdsAndGroupBuyStatuses(
            storeIds = storeIds,
            groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
            participationStatuses = listOf(ParticipationStatus.CONFIRMED),
        )
        if (hasPendingCustomerPickup) {
            throw CustomException(ErrorCode.OWNER_WITHDRAWAL_BLOCKED_PENDING_CUSTOMER_PICKUP)
        }
    }

    private fun validateReason(request: OwnerWithdrawRequest) {
        if (request.reason == OwnerWithdrawalReason.OTHER && request.reasonDetail.isNullOrBlank()) {
            throw CustomException(ErrorCode.OWNER_WITHDRAWAL_REASON_DETAIL_REQUIRED)
        }
    }

    private fun normalizedReasonDetail(request: OwnerWithdrawRequest): String? {
        if (request.reason != OwnerWithdrawalReason.OTHER) {
            return null
        }
        return request.reasonDetail?.trim()?.takeIf { it.isNotBlank() }
    }
}
