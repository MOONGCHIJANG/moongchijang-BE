package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyCloseReason
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageDetailResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageFilterType
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyManageParticipantSummary
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyParticipantItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyCloseReasonType
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyCloseRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyExtensionRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyListItemResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuySummaryResponse
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.domain.entity.Payment
import com.moongchijang.domain.payment.domain.repository.PaymentRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.time.TimePolicy
import com.moongchijang.security.crypto.PersonalInfoManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class OwnerGroupBuyService(
    private val userRepository: UserRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val ownerGroupBuyRequestRepository: OwnerGroupBuyRequestRepository,
    private val participationRepository: ParticipationRepository,
    private val paymentRepository: PaymentRepository,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val personalInfoManager: PersonalInfoManager,
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
            pickupDate = LocalDate.now(SEOUL_ZONE_ID),
            participationStatuses = TODAY_PICKUP_PARTICIPATION_STATUSES,
            pickupStatuses = TODAY_PICKUP_PICKUP_STATUSES,
            groupBuyStatuses = TODAY_PICKUP_GROUP_BUY_STATUSES
        ).toInt()

        val settlementExpectedAmount = groupBuyRepository.sumSettlementExpectedAmountByStoreIds(
            storeIds = storeIds,
            participationStatuses = SETTLEMENT_PARTICIPATION_STATUSES,
            groupBuyStatuses = SETTLEMENT_GROUP_BUY_STATUSES
        )

        val isEmpty = ongoingCount == 0 &&
            achievedCount == 0 &&
            todayPickupUserCount == 0 &&
            settlementExpectedAmount == 0L

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

    fun getManageGroupBuys(ownerId: Long, filter: OwnerGroupBuyManageFilterType): List<OwnerGroupBuyManageListItemResponse> {
        log.info("[OwnerGroupBuyService] 사장님 공구 관리 목록 조회 시작: ownerId={}, filter={}", ownerId, filter)
        validateSeller(ownerId)

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            log.info("[OwnerGroupBuyService] 사장님 공구 관리 목록 조회 완료(소속 매장 없음): ownerId={}, filter={}", ownerId, filter)
            return emptyList()
        }

        val today = LocalDate.now(SEOUL_ZONE_ID)
        val response = when (filter) {
            OwnerGroupBuyManageFilterType.PENDING_APPROVAL -> {
                ownerGroupBuyRequestRepository
                    .findByOwnerIdAndStoreIdInAndStatusOrderByCreatedAtDesc(ownerId, storeIds, OwnerGroupBuyRequestStatus.PENDING)
                    .map {
                        OwnerGroupBuyManageListItemResponse(
                            requestId = it.id,
                            productName = it.productName,
                            price = it.price,
                            pickupDate = it.pickupDate,
                            status = OwnerGroupBuyManageFilterType.PENDING_APPROVAL
                        )
                    }
            }
            else -> {
                val statuses = toGroupBuyStatuses(filter)
                groupBuyRepository.findByStoreIdInAndStatusInOrderByDeadlineAsc(storeIds, statuses)
                    .map {
                        OwnerGroupBuyManageListItemResponse(
                            groupBuyId = it.id,
                            productName = it.productName,
                            price = it.price,
                            pickupDate = it.pickupDate,
                            deadlineDday = if (it.status == GroupBuyStatus.IN_PROGRESS) calculateDday(today, it.deadline.toLocalDate()) else null,
                            achievementRate = ((it.currentQuantity * 100L) / it.targetQuantity.coerceAtLeast(1)).toInt(),
                            currentQuantity = it.currentQuantity,
                            targetQuantity = it.targetQuantity,
                            status = toManageFilterType(it.status)
                        )
                    }
            }
        }

        log.info(
            "[OwnerGroupBuyService] 사장님 공구 관리 목록 조회 완료: ownerId={}, filter={}, count={}",
            ownerId,
            filter,
            response.size
        )
        return response
    }

    fun getInProgressGroupBuyDetail(ownerId: Long, groupBuyId: Long): OwnerGroupBuyManageDetailResponse {
        return getManageGroupBuyDetail(ownerId, groupBuyId, IN_PROGRESS_DETAIL_GROUP_BUY_STATUSES)
    }

    fun getAchievedGroupBuyDetail(ownerId: Long, groupBuyId: Long): OwnerGroupBuyManageDetailResponse {
        return getManageGroupBuyDetail(ownerId, groupBuyId, ACHIEVED_DETAIL_GROUP_BUY_STATUSES)
    }

    @Transactional
    fun requestGroupBuyExtension(ownerId: Long, groupBuyId: Long, request: OwnerGroupBuyExtensionRequest) {
        log.info(
            "[OwnerGroupBuyService] 사장님 공구 기간 연장 요청 시작: ownerId={}, groupBuyId={}, extendedDeadline={}",
            ownerId,
            groupBuyId,
            request.extendedDeadline
        )
        val groupBuy = findOwnedGroupBuy(ownerId, groupBuyId)
        if (groupBuy.status != GroupBuyStatus.IN_PROGRESS) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        if (!request.extendedDeadline.isAfter(groupBuy.deadline)) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        val pickupStartDateTime = java.time.LocalDateTime.of(groupBuy.pickupDate, groupBuy.pickupTimeStart)
        if (!request.extendedDeadline.isBefore(pickupStartDateTime)) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }

        groupBuy.deadline = request.extendedDeadline
        groupBuyRepository.save(groupBuy)
        log.info(
            "[OwnerGroupBuyService] 사장님 공구 기간 연장 요청 완료: ownerId={}, groupBuyId={}, newDeadline={}",
            ownerId,
            groupBuyId,
            groupBuy.deadline
        )
    }

    @Transactional
    fun requestGroupBuyClose(ownerId: Long, groupBuyId: Long, request: OwnerGroupBuyCloseRequest) {
        log.info(
            "[OwnerGroupBuyService] 사장님 공구 마감 요청 시작: ownerId={}, groupBuyId={}, reason={}",
            ownerId,
            groupBuyId,
            request.reason
        )
        val groupBuy = findOwnedGroupBuy(ownerId, groupBuyId)
        if (groupBuy.status !in CLOSE_ALLOWED_STATUSES) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        validateCloseReason(request)
        val requestedAt = java.time.LocalDateTime.now(SEOUL_ZONE_ID)
        val reason = toGroupBuyCloseReason(request.reason)
        val reasonDetail = request.reasonDetail?.trim()?.takeIf { it.isNotBlank() }

        if (request.reason == OwnerGroupBuyCloseReasonType.OTHER) {
            groupBuy.requestCloseReview(
                reason = reason,
                reasonDetail = reasonDetail,
                requestedAt = requestedAt
            )
        } else {
            groupBuy.closeByOwner(
                reason = reason,
                reasonDetail = reasonDetail,
                requestedAt = requestedAt
            )
        }
        groupBuyRepository.save(groupBuy)
        if (request.reason != OwnerGroupBuyCloseReasonType.OTHER) {
            notificationEventPublisher.publishOwnerCloseRequestApproved(
                groupBuyId = groupBuy.id,
                ownerUserIds = listOf(ownerId),
                occurredAt = requestedAt
            )
        }
        log.info(
            "[OwnerGroupBuyService] 사장님 공구 마감 요청 완료: ownerId={}, groupBuyId={}, reason={}, reasonDetail={}, reviewStatus={}",
            ownerId,
            groupBuyId,
            request.reason,
            request.reasonDetail,
            groupBuy.closeRequestReviewStatus
        )
    }

    private fun getManageGroupBuyDetail(
        ownerId: Long,
        groupBuyId: Long,
        allowedStatuses: Collection<GroupBuyStatus>
    ): OwnerGroupBuyManageDetailResponse {
        log.info(
            "[OwnerGroupBuyService] 사장님 공구 관리 상세 조회 시작: ownerId={}, groupBuyId={}, allowedStatuses={}",
            ownerId,
            groupBuyId,
            allowedStatuses
        )
        validateSeller(ownerId)
        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val groupBuy = groupBuyRepository.findWithStoreById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

        if (groupBuy.store.id !in storeIds || groupBuy.status !in allowedStatuses) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val participations = participationRepository.findByGroupBuyIdAndStatusInOrderByCreatedAtAsc(
            groupBuyId = groupBuyId,
            statuses = DETAIL_PARTICIPATION_STATUSES
        )
        val paymentByUserId = mapPaymentsByUserId(groupBuyId, participations)
        val participants = participations.map {
            val payment = paymentByUserId[it.user.id!!]
            OwnerGroupBuyParticipantItemResponse(
                name = it.user.nickname ?: "",
                phoneNumber = personalInfoManager.decryptIfNeeded(it.user.phoneNumber) ?: "",
                productName = it.groupBuy.productName,
                quantity = it.quantity,
                paymentMethod = payment?.method ?: UNKNOWN_PAYMENT_METHOD,
                paymentStatus = payment?.status?.name ?: UNKNOWN_PAYMENT_STATUS,
                pickupTime = it.groupBuy.pickupTimeStart
            )
        }

        val totalCount = participations.size
        val completedCount = participations.count { it.pickupStatus == PickupStatus.PICKED_UP }
        val waitingCount = totalCount - completedCount

        val response = OwnerGroupBuyManageDetailResponse(
            groupBuyId = groupBuy.id,
            status = toManageFilterType(groupBuy.status),
            recruitmentStartDate = groupBuy.recruitmentStartAt.toLocalDate(),
            recruitmentEndDate = groupBuy.deadline.toLocalDate(),
            participantSummary = OwnerGroupBuyManageParticipantSummary(
                totalCount = totalCount,
                completedCount = completedCount,
                waitingCount = waitingCount
            ),
            participants = participants
        )

        log.info(
            "[OwnerGroupBuyService] 사장님 공구 관리 상세 조회 완료: ownerId={}, groupBuyId={}, totalCount={}",
            ownerId,
            groupBuyId,
            response.participantSummary.totalCount
        )
        return response
    }

    private fun findOwnedGroupBuy(ownerId: Long, groupBuyId: Long): com.moongchijang.domain.groupbuy.domain.entity.GroupBuy {
        validateSeller(ownerId)
        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val groupBuy = groupBuyRepository.findWithStoreById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

        if (groupBuy.store.id !in storeIds) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
        return groupBuy
    }

    private fun validateCloseReason(request: OwnerGroupBuyCloseRequest) {
        if (request.reason == OwnerGroupBuyCloseReasonType.OTHER && request.reasonDetail.isNullOrBlank()) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun toGroupBuyCloseReason(reasonType: OwnerGroupBuyCloseReasonType): GroupBuyCloseReason {
        return when (reasonType) {
            OwnerGroupBuyCloseReasonType.SOLD_OUT -> GroupBuyCloseReason.SOLD_OUT
            OwnerGroupBuyCloseReasonType.STORE_CONDITION -> GroupBuyCloseReason.STORE_CONDITION
            OwnerGroupBuyCloseReasonType.OTHER -> GroupBuyCloseReason.OTHER
        }
    }

    private fun mapPaymentsByUserId(groupBuyId: Long, participations: List<Participation>): Map<Long, Payment> {
        val userIds = participations.mapNotNull { it.user.id }.distinct()
        if (userIds.isEmpty()) {
            return emptyMap()
        }
        return paymentRepository.findAllByGroupBuyIdAndUserIdIn(groupBuyId, userIds)
            .associateBy { it.paymentOrder.user.id ?: error("paymentOrder.user.id must not be null") }
    }

    private fun toGroupBuyStatuses(filter: OwnerGroupBuyManageFilterType): Collection<GroupBuyStatus> {
        return when (filter) {
            OwnerGroupBuyManageFilterType.ALL -> OWNER_VISIBLE_STATUSES
            OwnerGroupBuyManageFilterType.IN_PROGRESS -> listOf(GroupBuyStatus.IN_PROGRESS)
            OwnerGroupBuyManageFilterType.ACHIEVED -> listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
            OwnerGroupBuyManageFilterType.ENDED -> listOf(GroupBuyStatus.FAILED, GroupBuyStatus.CLOSED)
            OwnerGroupBuyManageFilterType.PENDING_APPROVAL -> emptyList()
        }
    }

    private fun toManageFilterType(status: GroupBuyStatus): OwnerGroupBuyManageFilterType {
        return when (status) {
            GroupBuyStatus.IN_PROGRESS -> OwnerGroupBuyManageFilterType.IN_PROGRESS
            GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED -> OwnerGroupBuyManageFilterType.ACHIEVED
            GroupBuyStatus.FAILED, GroupBuyStatus.CLOSED -> OwnerGroupBuyManageFilterType.ENDED
        }
    }

    private fun calculateDday(today: LocalDate, deadlineDate: LocalDate): Int {
        return ChronoUnit.DAYS.between(today, deadlineDate).toInt()
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
        settlementExpectedAmount = 0L,
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
        val IN_PROGRESS_DETAIL_GROUP_BUY_STATUSES = listOf(GroupBuyStatus.IN_PROGRESS)
        val ACHIEVED_DETAIL_GROUP_BUY_STATUSES = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED)
        val CLOSE_ALLOWED_STATUSES = listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED)
        val DETAIL_PARTICIPATION_STATUSES = listOf(ParticipationStatus.PAID_WAITING_GOAL, ParticipationStatus.CONFIRMED)
        const val UNKNOWN_PAYMENT_METHOD = "UNKNOWN"
        const val UNKNOWN_PAYMENT_STATUS = "UNKNOWN"
        val SEOUL_ZONE_ID: ZoneId = TimePolicy.BUSINESS_ZONE_ID
    }
}
