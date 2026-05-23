package com.moongchijang.domain.notification.application.template

import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType

data class NotificationTemplateRenderResult(
    val title: String,
    val body: String,
    val deeplinkType: NotificationDeeplinkType,
)
