package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.event.NotificationImmediateTriggerEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class NotificationImmediateTriggerEventListener(
    private val notificationImmediateDispatchService: NotificationImmediateDispatchService
) {

    @EventListener
    fun on(event: NotificationImmediateTriggerEvent) {
        notificationImmediateDispatchService.dispatch(event)
    }
}
