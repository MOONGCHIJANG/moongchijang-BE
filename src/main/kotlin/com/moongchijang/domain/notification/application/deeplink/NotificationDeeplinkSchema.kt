package com.moongchijang.domain.notification.application.deeplink

import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType

object NotificationDeeplinkSchema {

    fun toParams(deeplinkType: NotificationDeeplinkType, targetId: Long?): Map<String, String> {
        if (targetId == null) {
            return emptyMap()
        }

        return when (deeplinkType) {
            NotificationDeeplinkType.PICKUP_GUIDE -> mapOf("participationId" to targetId.toString())
            NotificationDeeplinkType.GROUPBUY_DETAIL -> mapOf("groupBuyId" to targetId.toString())
            NotificationDeeplinkType.MY_APPLYING -> mapOf("groupBuyId" to targetId.toString())
            NotificationDeeplinkType.REQUEST_STATUS -> mapOf("requestId" to targetId.toString())
        }
    }
}
