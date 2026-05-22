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
        targetGroupBuyId: Long,
        requesterUserIds: List<Long>,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.REQUEST_OPENED_IMMEDIATE,
            targetId = targetGroupBuyId,
            userIds = requesterUserIds,
            scheduleKey = "request-opened:$targetGroupBuyId",
            occurredAt = occurredAt
        )
    }

    fun publishRequestNewParticipant(
        targetGroupBuyId: Long,
        requesterUserId: Long,
        participationId: Long,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.REQUEST_NEW_PARTICIPANT_IMMEDIATE,
            targetId = targetGroupBuyId,
            userIds = listOf(requesterUserId),
            scheduleKey = "request-new-participant:$targetGroupBuyId:$participationId",
            occurredAt = occurredAt
        )
    }

    fun publishRequestTargetAchieved(
        targetGroupBuyId: Long,
        requesterUserId: Long,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.REQUEST_TARGET_ACHIEVED_IMMEDIATE,
            targetId = targetGroupBuyId,
            userIds = listOf(requesterUserId),
            scheduleKey = "request-target-achieved:$targetGroupBuyId",
            occurredAt = occurredAt
        )
    }

    fun publishPickupCompleted(
        groupBuyId: Long,
        userId: Long,
        participationId: Long,
        occurredAt: LocalDateTime
    ) {
        publish(
            triggerType = NotificationTriggerType.PICKUP_COMPLETED_IMMEDIATE,
            targetId = groupBuyId,
            userIds = listOf(userId),
            scheduleKey = "pickup-completed:$groupBuyId:$participationId",
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
