package com.moongchijang.domain.notification.application.event

import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import java.time.LocalDateTime

data class NotificationImmediateTriggerEvent(
    val triggerType: NotificationTriggerType,
    val targetId: Long,
    val userIds: List<Long>,
    val scheduleKey: String,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
