package com.moongchijang.domain.notification.application.discord

import com.moongchijang.domain.notification.application.discord.event.AdminDiscordAlertRequestedEvent
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher

class AdminDiscordAlertServiceTest {

    @Test
    fun `새 공구 요청 알림을 보낼 때 온보딩 채널로 전송됨`() {
        val publisher = mock(ApplicationEventPublisher::class.java)
        val service = AdminDiscordAlertService(publisher)
        val request = GroupBuyFixture.createGroupBuyRequest(storeName = "몽치장베이커리", productName = "소금빵", desiredQuantity = 3)

        service.sendNewGroupBuyRequest(request)

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.ONBOARDING, captor.value.channel)
        assertTrue(captor.value.message.contains("[새 요청]"))
    }

    @Test
    fun `공구 달성 알림을 보낼 때 공구 채널로 전송되고 금액 문구가 포함됨`() {
        val publisher = mock(ApplicationEventPublisher::class.java)
        val service = AdminDiscordAlertService(publisher)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 11L,
            status = com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus.ACHIEVED,
            productName = "소금빵",
            price = 5000,
            currentQuantity = 20,
        )

        service.sendGroupBuyAchieved(groupBuy, participantCount = 18)

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.GROUPBUY, captor.value.channel)
        assertTrue(captor.value.message.contains("총 100,000원"))
    }

    @Test
    fun `결제 실패 알림을 보낼 때 결제 채널로 전송됨`() {
        val publisher = mock(ApplicationEventPublisher::class.java)
        val service = AdminDiscordAlertService(publisher)

        service.sendPaymentFailed(
            orderId = "MCJ-10-test",
            pgPaymentId = "MCJ-10-test",
            pgStatus = "FAILED",
            reason = "PAYMENT_APPROVAL_FAILED",
        )

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.PAYMENT, captor.value.channel)
        assertTrue(captor.value.message.contains("결제 실패"))
        assertTrue(captor.value.message.contains("MCJ-10-test"))
    }
}
