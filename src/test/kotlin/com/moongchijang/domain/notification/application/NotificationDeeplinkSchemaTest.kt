package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.deeplink.NotificationDeeplinkSchema
import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NotificationDeeplinkSchemaTest {

    @Test
    fun `PICKUP_GUIDE 딥링크 파라미터는 groupBuyId 반환`() {
        val params = NotificationDeeplinkSchema.toParams(NotificationDeeplinkType.PICKUP_GUIDE, 11L)
        assertEquals("11", params["groupBuyId"])
    }

    @Test
    fun `GROUPBUY_DETAIL 딥링크 파라미터는 groupBuyId 반환`() {
        val params = NotificationDeeplinkSchema.toParams(NotificationDeeplinkType.GROUPBUY_DETAIL, 12L)
        assertEquals("12", params["groupBuyId"])
    }

    @Test
    fun `MY_APPLYING 딥링크 파라미터는 groupBuyId 반환`() {
        val params = NotificationDeeplinkSchema.toParams(NotificationDeeplinkType.MY_APPLYING, 13L)
        assertEquals("13", params["groupBuyId"])
    }

    @Test
    fun `REQUEST_STATUS 딥링크 파라미터는 targetId 반환`() {
        val params = NotificationDeeplinkSchema.toParams(NotificationDeeplinkType.REQUEST_STATUS, 14L)
        assertEquals("14", params["targetId"])
    }

    @Test
    fun `targetId가 null이면 빈 파라미터 반환`() {
        val params = NotificationDeeplinkSchema.toParams(NotificationDeeplinkType.REQUEST_STATUS, null)
        assertTrue(params.isEmpty())
    }
}
