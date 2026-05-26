package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuySummaryResponse
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class OwnerGroupBuyService(
    private val userRepository: UserRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val groupBuyRepository: GroupBuyRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getMyGroupBuys(ownerId: Long): List<OwnerGroupBuyListItemResponse> {
        log.info("[OwnerGroupBuyService] 사장님 공구 목록 조회 시작: ownerId={}", ownerId)
        validateSeller(ownerId)

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            log.info("[OwnerGroupBuyService] 사장님 공구 목록 조회 완료(소속 매장 없음): ownerId={}", ownerId)
            return emptyList()
        }

        val response = groupBuyRepository
            .findByStoreIdInAndStatusInOrderByDeadlineAsc(storeIds, OWNER_VISIBLE_STATUSES)
            .map { OwnerGroupBuyListItemResponse.from(it) }

        log.info(
            "[OwnerGroupBuyService] 사장님 공구 목록 조회 완료: ownerId={}, storeCount={}, groupBuyCount={}",
            ownerId,
            storeIds.size,
            response.size
        )
        return response
    }

    fun getMyGroupBuySummary(ownerId: Long): OwnerGroupBuySummaryResponse {
        log.info("[OwnerGroupBuyService] 사장님 공구 요약 조회 시작: ownerId={}", ownerId)
        validateSeller(ownerId)

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            log.info("[OwnerGroupBuyService] 사장님 공구 요약 조회 완료(소속 매장 없음): ownerId={}", ownerId)
            return emptySummary()
        }

        val ongoingCount = groupBuyRepository.countByStoreIdsAndStatuses(
            storeIds = storeIds,
            statuses = ONGOING_STATUSES
        ).toInt()

        val achievedCount = groupBuyRepository.countByStoreIdsAndStatuses(
            storeIds = storeIds,
            statuses = ACHIEVED_STATUSES
        ).toInt()

        val todayPickupUserCount = groupBuyRepository.countTodayPickupUsersByStoreIds(
            storeIds = storeIds,
            pickupDate = LocalDate.now(),
            participationStatuses = TODAY_PICKUP_PARTICIPATION_STATUSES,
            pickupStatuses = TODAY_PICKUP_PICKUP_STATUSES,
            groupBuyStatuses = TODAY_PICKUP_GROUP_BUY_STATUSES
        ).toInt()

        val settlementExpectedAmount = groupBuyRepository.sumSettlementExpectedAmountByStoreIds(
            storeIds = storeIds,
            participationStatuses = SETTLEMENT_PARTICIPATION_STATUSES,
            groupBuyStatuses = SETTLEMENT_GROUP_BUY_STATUSES
        ).toInt()

        val isEmpty = ongoingCount == 0 &&
            achievedCount == 0 &&
            todayPickupUserCount == 0 &&
            settlementExpectedAmount == 0

        val response = OwnerGroupBuySummaryResponse(
            ongoingCount = ongoingCount,
            achievedCount = achievedCount,
            todayPickupUserCount = todayPickupUserCount,
            settlementExpectedAmount = settlementExpectedAmount,
            isEmpty = isEmpty
        )

        log.info(
            "[OwnerGroupBuyService] 사장님 공구 요약 조회 완료: ownerId={}, ongoingCount={}, achievedCount={}, todayPickupUserCount={}, settlementExpectedAmount={}, isEmpty={}",
            ownerId,
            response.ongoingCount,
            response.achievedCount,
            response.todayPickupUserCount,
            response.settlementExpectedAmount,
            response.isEmpty
        )
        return response
    }

    private fun validateSeller(ownerId: Long): User {
        val owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (owner.role != UserRole.SELLER) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
        return owner
    }

    private fun emptySummary() = OwnerGroupBuySummaryResponse(
        ongoingCount = 0,
        achievedCount = 0,
        todayPickupUserCount = 0,
        settlementExpectedAmount = 0,
        isEmpty = true
    )

    private companion object {
        val OWNER_VISIBLE_STATUSES = listOf(
            GroupBuyStatus.IN_PROGRESS,
            GroupBuyStatus.ACHIEVED,
            GroupBuyStatus.COMPLETED,
            GroupBuyStatus.FAILED,
            GroupBuyStatus.CLOSED
        )
        val ONGOING_STATUSES = listOf(GroupBuyStatus.IN_PROGRESS)
        val ACHIEVED_STATUSES = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
        val TODAY_PICKUP_PARTICIPATION_STATUSES = listOf(ParticipationStatus.CONFIRMED)
        val TODAY_PICKUP_PICKUP_STATUSES = listOf(PickupStatus.NOT_READY, PickupStatus.READY)
        val TODAY_PICKUP_GROUP_BUY_STATUSES = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
        val SETTLEMENT_PARTICIPATION_STATUSES = listOf(ParticipationStatus.CONFIRMED)
        val SETTLEMENT_GROUP_BUY_STATUSES = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
    }
}
