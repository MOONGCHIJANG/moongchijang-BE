package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.template.NotificationTemplateRenderer
import com.moongchijang.domain.notification.application.template.NotificationTemplateRegistry
import com.moongchijang.domain.notification.application.template.NotificationTemplateType
import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NotificationTemplateRendererTest {

    private val registry = NotificationTemplateRegistry()
    private val renderer = NotificationTemplateRenderer()

    @Test
    fun `픽업 전일 템플릿 렌더링 시 줄바꿈 포함 본문 반환`() {
        val rendered = renderer.render(
            template = registry.getTemplateByType(NotificationTemplateType.PICKUP_DAY_BEFORE_REMINDER),
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
        val exception = assertThrows<CustomException> {
            renderer.render(
                template = registry.getTemplateByType(NotificationTemplateType.REQUEST_NEW_PARTICIPANT),
                variables = mapOf(
                    "상품명" to "소금빵",
                    "현재참여개수" to "3"
                )
            )
        }

        assertEquals(ErrorCode.NOTIFICATION_TEMPLATE_VARIABLE_MISSING, exception.errorCode)
        assertTrue(exception.detail!!.contains("목표참여개수"))
    }

    @Test
    fun `달성 템플릿 렌더링 시 픽업 정보 포함 본문 반환`() {
        val rendered = renderer.render(
            template = registry.getTemplateByType(NotificationTemplateType.APPLY_GROUPBUY_ACHIEVED),
            variables = mapOf(
                "상품명" to "소금빵",
                "픽업일자" to "2026-05-30",
                "픽업시간범위" to "09:00 ~ 12:00",
                "매장명" to "성수베이커리",
                "매장주소" to "서울 성동구 성수이로 1"
            )
        )

        assertTrue(rendered.body.contains("픽업: 2026-05-30 09:00 ~ 12:00"))
        assertTrue(rendered.body.contains("매장: 성수베이커리"))
        assertTrue(rendered.body.contains("주소: 서울 성동구 성수이로 1"))
    }

    @Test
    fun `미달성 템플릿 렌더링 시 환불 예상 시각 포함 본문 반환`() {
        val rendered = renderer.render(
            template = registry.getTemplateByType(NotificationTemplateType.APPLY_GROUPBUY_FAILED),
            variables = mapOf(
                "상품명" to "소금빵",
                "환불예상시각" to "2026-05-28 10:00"
            )
        )

        assertTrue(rendered.body.contains("2026-05-28 10:00"))
    }

    @Test
    fun `사장님 발주 취소 템플릿 렌더링 시 패널티 문구 없이 본문 반환`() {
        val rendered = renderer.render(
            template = registry.getTemplateByType(NotificationTemplateType.OWNER_ORDER_CANCELLED),
            variables = mapOf(
                "상품명" to "소금빵"
            )
        )

        assertEquals("발주가 취소됐어요.", rendered.title)
        assertEquals("소금빵 발주 미확정으로 인해 발주가 취소됐어요.", rendered.body)
        assertTrue(rendered.body.contains("발주 미확정"))
    }
}
