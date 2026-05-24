package com.moongchijang.support

import com.moongchijang.domain.notification.domain.entity.Notification
import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.domain.entity.NotificationType
import com.moongchijang.domain.user.domain.entity.User
import java.time.LocalDateTime

object NotificationFixture {

    fun createNotification(
        user: User,
        id: Long = 0L,
        type: NotificationType = NotificationType.PICKUP,
        title: String = "알림 제목",
        body: String = "알림 본문",
        isRead: Boolean = false,
        occurredAt: LocalDateTime = LocalDateTime.now(),
        targetId: Long? = null,
        deeplinkType: NotificationDeeplinkType = NotificationDeeplinkType.PICKUP_GUIDE,
        triggerType: NotificationTriggerType? = null,
    ): Notification {
        return Notification(
            user = user,
            type = type,
            title = title,
            body = body,
            isRead = isRead,
            occurredAt = occurredAt,
            targetId = targetId,
            deeplinkType = deeplinkType,
            triggerType = triggerType,
            id = id,
        )
    }
}
