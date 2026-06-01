package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupBuyRequestStatusCommandService(
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository,
    private val notificationEventPublisher: NotificationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun rejectRequest(
        requestId: Long,
        reason: String?,
        changedAt: LocalDateTime = LocalDateTime.now()
    ) {
        val request = groupBuyRequestRepository.findById(requestId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND) }

        request.markRejected(reason)
        groupBuyRequestStatusHistoryRepository.save(
            GroupBuyRequestStatusHistory(
                groupBuyRequestId = request.id,
                status = GroupBuyRequestStatus.REJECTED,
                changedAt = changedAt
            )
        )

        notificationEventPublisher.publishRequestRejected(
            requestId = request.id,
            requesterUserId = requireNotNull(request.user.id) { "GroupBuyRequest.user.id must not be null" },
            occurredAt = changedAt
        )
        log.info(
            "[GroupBuyRequestStatusCommandService] 요청공구 거절 처리 및 알림 트리거 발행: requestId={}, requesterUserId={}",
            requestId, requireNotNull(request.user.id) { "GroupBuyRequest.user.id must not be null" }
        )
    }
}
