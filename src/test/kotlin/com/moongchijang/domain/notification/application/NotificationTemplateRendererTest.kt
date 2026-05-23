package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.template.NotificationTemplateRenderer
import com.moongchijang.domain.notification.application.template.NotificationTemplateRegistry
import com.moongchijang.domain.notification.application.template.NotificationTemplateType
import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NotificationTemplateRendererTest {

    private val registry = NotificationTemplateRegistry()
    private val renderer = NotificationTemplateRenderer(registry)

    @Test
    fun `픽업 전일 템플릿 렌더링 시 줄바꿈 포함 본문 반환`() {
        val rendered = renderer.render(
            templateType = NotificationTemplateType.PICKUP_DAY_BEFORE_REMINDER,
            variables = mapOf(
                "상품명" to "소금빵",
                "픽업시간범위" to "10:00 ~ 12:00"
            )
        )

        assertEquals("내일 픽업일이에요. 잊지마세요!", rendered.title)
        assertEquals("소금빵 픽업이\n내일 10:00 ~ 12:00로 예정됐어요.", rendered.body)
        assertEquals(NotificationDeeplinkType.PICKUP_GUIDE, rendered.deeplinkType)
    }

    @Test
    fun `필수 템플릿 변수가 누락될 때 예외 발생`() {
        val exception = assertThrows<IllegalArgumentException> {
            renderer.render(
                templateType = NotificationTemplateType.REQUEST_NEW_PARTICIPANT,
                variables = mapOf(
                    "상품명" to "소금빵",
                    "현재참여개수" to "3"
                )
            )
        }

        assertTrue(exception.message!!.contains("목표참여개수"))
    }
}
