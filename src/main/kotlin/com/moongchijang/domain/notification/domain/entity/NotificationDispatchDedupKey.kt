package com.moongchijang.domain.notification.domain.entity

data class NotificationDispatchDedupKey(
    val userId: Long,
    val triggerType: NotificationTriggerType,
    val targetId: Long,
    val scheduleKey: String
) {
    companion object {
        fun of(
            userId: Long,
            triggerType: NotificationTriggerType,
            targetId: Long,
            scheduleKey: String
        ): NotificationDispatchDedupKey {
            return NotificationDispatchDedupKey(
                userId = userId,
                triggerType = triggerType,
                targetId = targetId,
                scheduleKey = scheduleKey
            )
        }
    }
}
