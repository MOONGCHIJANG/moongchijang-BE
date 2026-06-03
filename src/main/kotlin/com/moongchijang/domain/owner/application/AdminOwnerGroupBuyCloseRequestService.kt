package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyCloseReason
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyCloseRequestReviewStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyCloseRequestActionResponse
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyCloseRequestRejectRequest
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
class AdminOwnerGroupBuyCloseRequestService(
    private val groupBuyRepository: GroupBuyRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun approve(groupBuyId: Long): AdminOwnerGroupBuyCloseRequestActionResponse {
        log.info("[AdminOwnerGroupBuyCloseRequestService] 사장님 공구 마감 요청 승인 시작: groupBuyId={}", groupBuyId)
        val groupBuy = findGroupBuyForReview(groupBuyId)
        validatePendingReview(groupBuy)

        val reviewedAt = LocalDateTime.now(clock)
        groupBuy.approveCloseReview(reviewedAt)
        publishApproved(groupBuy, reviewedAt)

        val response = AdminOwnerGroupBuyCloseRequestActionResponse(
            groupBuyId = groupBuy.id,
            reviewStatus = GroupBuyCloseRequestReviewStatus.APPROVED,
            groupBuyStatus = groupBuy.status
        )
        log.info(
            "[AdminOwnerGroupBuyCloseRequestService] 사장님 공구 마감 요청 승인 완료: groupBuyId={}, status={}, reviewStatus={}",
            groupBuyId,
            groupBuy.status,
            groupBuy.closeRequestReviewStatus
        )
        return response
    }

    fun reject(groupBuyId: Long, request: AdminOwnerGroupBuyCloseRequestRejectRequest): AdminOwnerGroupBuyCloseRequestActionResponse {
        log.info("[AdminOwnerGroupBuyCloseRequestService] 사장님 공구 마감 요청 반려 시작: groupBuyId={}", groupBuyId)
        val groupBuy = findGroupBuyForReview(groupBuyId)
        validatePendingReview(groupBuy)

        val reason = request.rejectionReason.trim()
        if (reason.isBlank()) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_REJECTION_REASON_REQUIRED)
        }

        val reviewedAt = LocalDateTime.now(clock)
        groupBuy.rejectCloseReview(reason, reviewedAt)
        publishRejected(groupBuy, reviewedAt)

        val response = AdminOwnerGroupBuyCloseRequestActionResponse(
            groupBuyId = groupBuy.id,
            reviewStatus = GroupBuyCloseRequestReviewStatus.REJECTED,
            groupBuyStatus = groupBuy.status
        )
        log.info(
            "[AdminOwnerGroupBuyCloseRequestService] 사장님 공구 마감 요청 반려 완료: groupBuyId={}, status={}, reviewStatus={}",
            groupBuyId,
            groupBuy.status,
            groupBuy.closeRequestReviewStatus
        )
        return response
    }

    private fun findGroupBuyForReview(groupBuyId: Long): GroupBuy =
        groupBuyRepository.findWithLockById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

    private fun validatePendingReview(groupBuy: GroupBuy) {
        if (groupBuy.closeReason != GroupBuyCloseReason.OTHER ||
            groupBuy.closeRequestReviewStatus != GroupBuyCloseRequestReviewStatus.PENDING ||
            groupBuy.status !in listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED)
        ) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION)
        }
    }

    private fun publishApproved(groupBuy: GroupBuy, occurredAt: LocalDateTime) {
        val ownerUserIds = storeStaffRepository.findUserIdsByStoreId(groupBuy.store.id).distinct()
        if (ownerUserIds.isEmpty()) return
        notificationEventPublisher.publishOwnerCloseRequestApproved(
            groupBuyId = groupBuy.id,
            ownerUserIds = ownerUserIds,
            occurredAt = occurredAt
        )
    }

    private fun publishRejected(groupBuy: GroupBuy, occurredAt: LocalDateTime) {
        val ownerUserIds = storeStaffRepository.findUserIdsByStoreId(groupBuy.store.id).distinct()
        if (ownerUserIds.isEmpty()) return
        notificationEventPublisher.publishOwnerCloseRequestRejected(
            groupBuyId = groupBuy.id,
            ownerUserIds = ownerUserIds,
            occurredAt = occurredAt
        )
    }
}
