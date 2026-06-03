package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.event.NotificationImmediateTriggerEvent
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NotificationEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun publishApplyPaymentSuccess(
        groupBuyId: Long,
        orderId: String,
        userId: Long,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.APPLY_PAYMENT_SUCCESS_IMMEDIATE,
            targetId = groupBuyId,
            userIds = listOf(userId),
            scheduleKey = "payment-success:$orderId",
            occurredAt = occurredAt
        )
    }

    fun publishApplyGroupBuyAchieved(
        groupBuyId: Long,
        participantUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.APPLY_GROUPBUY_ACHIEVED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = participantUserIds,
            scheduleKey = "groupbuy-achieved:$groupBuyId",
            occurredAt = occurredAt
        )
    }

    fun publishWishTargetAchieved(
        groupBuyId: Long,
        userIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.WISH_TARGET_ACHIEVED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = userIds,
            scheduleKey = "wish-target-achieved:$groupBuyId",
            occurredAt = occurredAt
        )
    }

    fun publishApplyGroupBuyFailed(
        groupBuyId: Long,
        participantUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.APPLY_GROUPBUY_FAILED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = participantUserIds,
            scheduleKey = "groupbuy-failed:$groupBuyId",
            occurredAt = occurredAt
        )
    }

    fun publishRequestOpened(
        requestId: Long,
        requesterUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.REQUEST_OPENED_IMMEDIATE,
            targetId = requestId,
            userIds = requesterUserIds,
            scheduleKey = "request-opened:$requestId",
            occurredAt = occurredAt
        )
    }

    fun publishRequestNewParticipant(
        requestId: Long,
        requesterUserId: Long,
        participationId: Long,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.REQUEST_NEW_PARTICIPANT_IMMEDIATE,
            targetId = requestId,
            userIds = listOf(requesterUserId),
            scheduleKey = "request-new-participant:$requestId:$participationId",
            occurredAt = occurredAt
        )
    }

    fun publishRequestTargetAchieved(
        requestId: Long,
        requesterUserId: Long,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.REQUEST_TARGET_ACHIEVED_IMMEDIATE,
            targetId = requestId,
            userIds = listOf(requesterUserId),
            scheduleKey = "request-target-achieved:$requestId",
            occurredAt = occurredAt
        )
    }

    fun publishRequestRejected(
        requestId: Long,
        requesterUserId: Long,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.REQUEST_REJECTED_IMMEDIATE,
            targetId = requestId,
            userIds = listOf(requesterUserId),
            scheduleKey = "request-rejected:$requestId",
            occurredAt = occurredAt
        )
    }

    fun publishOwnerGroupBuyAchieved(
        groupBuyId: Long,
        ownerUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.OWNER_GROUPBUY_ACHIEVED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = ownerUserIds,
            scheduleKey = "owner-groupbuy-achieved:$groupBuyId",
            occurredAt = occurredAt
        )
    }

    fun publishOwnerGroupBuyFailed(
        groupBuyId: Long,
        ownerUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.OWNER_GROUPBUY_FAILED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = ownerUserIds,
            scheduleKey = "owner-groupbuy-failed:$groupBuyId",
            occurredAt = occurredAt
        )
    }

    fun publishOwnerCloseRequestApproved(
        groupBuyId: Long,
        ownerUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.OWNER_CLOSE_REQUEST_APPROVED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = ownerUserIds,
            scheduleKey = "owner-close-approved:$groupBuyId:$occurredAt",
            occurredAt = occurredAt
        )
    }

    fun publishOwnerCloseRequestRejected(
        groupBuyId: Long,
        ownerUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.OWNER_CLOSE_REQUEST_REJECTED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = ownerUserIds,
            scheduleKey = "owner-close-rejected:$groupBuyId:$occurredAt",
            occurredAt = occurredAt
        )
    }

    fun publishOwnerOpenRequestApproved(
        requestId: Long,
        ownerUserId: Long,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.OWNER_OPEN_REQUEST_APPROVED_IMMEDIATE,
            targetId = requestId,
            userIds = listOf(ownerUserId),
            scheduleKey = "owner-open-approved:$requestId",
            occurredAt = occurredAt
        )
    }

    fun publishOwnerOpenRequestRejected(
        requestId: Long,
        ownerUserId: Long,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.OWNER_OPEN_REQUEST_REJECTED_IMMEDIATE,
            targetId = requestId,
            userIds = listOf(ownerUserId),
            scheduleKey = "owner-open-rejected:$requestId",
            occurredAt = occurredAt
        )
    }

    fun publishOwnerOrderConfirmRequired(
        groupBuyId: Long,
        ownerUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.OWNER_ORDER_CONFIRM_REQUIRED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = ownerUserIds,
            scheduleKey = "owner-order-confirm-required:$groupBuyId",
            occurredAt = occurredAt
        )
    }

    fun publishOwnerOrderCancelled(
        groupBuyId: Long,
        ownerUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.OWNER_ORDER_CANCELLED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = ownerUserIds,
            scheduleKey = "owner-order-cancelled:$groupBuyId",
            occurredAt = occurredAt
        )
    }

    fun publishScheduledTrigger(
        triggerType: NotificationTriggerType,
        targetId: Long,
        userIds: List<Long>,
        scheduleKey: String,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = triggerType,
            targetId = targetId,
            userIds = userIds,
            scheduleKey = scheduleKey,
            occurredAt = occurredAt
        )
    }

    private fun publish(
        triggerType: NotificationTriggerType,
        targetId: Long,
        userIds: List<Long>,
        scheduleKey: String,
        occurredAt: LocalDateTime
    ) {
        applicationEventPublisher.publishEvent(
            NotificationImmediateTriggerEvent(
                triggerType = triggerType,
                targetId = targetId,
                userIds = userIds,
                scheduleKey = scheduleKey,
                occurredAt = occurredAt
            )
        )
    }
}
