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
