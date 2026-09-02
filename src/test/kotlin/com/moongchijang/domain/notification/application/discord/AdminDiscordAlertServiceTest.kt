package com.moongchijang.domain.notification.application.discord

import com.moongchijang.domain.notification.application.discord.event.AdminDiscordAlertRequestedEvent
import com.moongchijang.domain.notification.infrastructure.discord.DiscordProperties
import com.moongchijang.domain.user.domain.entity.SellerBusinessProfile
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
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
        val service = AdminDiscordAlertService(
            eventPublisher = publisher,
            discordProperties = DiscordProperties(adminBaseUrl = "https://app.moongchijang.com/admin")
        )
        val request = GroupBuyFixture.createGroupBuyRequest(storeName = "몽치장베이커리", productName = "소금빵", desiredQuantity = 3)

        service.sendNewGroupBuyRequest(request)

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.ONBOARDING, captor.value.channel)
        assertTrue(captor.value.message.contains("[새 요청]"))
        assertTrue(captor.value.message.contains("→ https://app.moongchijang.com/admin/group-buy-requests/20"))
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
        assertTrue(captor.value.message.contains("픽업일:"))
        assertTrue(captor.value.message.contains("→ 발주 확정 필요"))
    }

    @Test
    fun `미달성 위험 알림을 보낼 때 공구 채널로 전송되고 달성률이 포함됨`() {
        val publisher = mock(ApplicationEventPublisher::class.java)
        val service = AdminDiscordAlertService(publisher)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 12L,
            status = com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus.IN_PROGRESS,
            productName = "소금빵",
            targetQuantity = 50,
            currentQuantity = 36,
        )

        service.sendGroupBuyDeadlineRisk(groupBuy)

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.GROUPBUY, captor.value.channel)
        assertTrue(captor.value.message.contains("[주의]"))
        assertTrue(captor.value.message.contains("마감 24시간 전 / 현재 달성률 72%"))
        assertTrue(captor.value.message.contains("→ 조치 필요"))
    }

    @Test
    fun `공구 미달성 알림을 보낼 때 공구 채널로 전송되고 자동 환불 문구가 포함됨`() {
        val publisher = mock(ApplicationEventPublisher::class.java)
        val service = AdminDiscordAlertService(publisher)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 13L,
            status = com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus.FAILED,
            productName = "소금빵",
        )

        service.sendGroupBuyFailed(groupBuy, participantCount = 7)

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.GROUPBUY, captor.value.channel)
        assertTrue(captor.value.message.contains("[미달성]"))
        assertTrue(captor.value.message.contains("참여자 7명 자동 환불 처리 중"))
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

    @Test
    fun `환불 실패 알림을 보낼 때 환불 채널로 전송되고 주문번호와 금액이 포함됨`() {
        val publisher = mock(ApplicationEventPublisher::class.java)
        val service = AdminDiscordAlertService(publisher)

        service.sendRefundFailed(orderId = "MCJ-10-test", amount = 12000)

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.REFUND, captor.value.channel)
        assertTrue(captor.value.message.contains("[긴급] 환불 실패 발생"))
        assertTrue(captor.value.message.contains("주문번호: MCJ-10-test / 금액: 12,000원"))
    }

    @Test
    fun `환불 실패 요약 알림을 보낼 때 환불 채널로 전송됨`() {
        val publisher = mock(ApplicationEventPublisher::class.java)
        val service = AdminDiscordAlertService(publisher)

        service.sendRefundFailedSummary(failedCount = 2)

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.REFUND, captor.value.channel)
        assertTrue(captor.value.message.contains("실패 건수: 2건"))
    }

    @Test
    fun `결제 성공 알림을 보낼 때 결제 채널로 전송되고 금액 문구가 포함됨`() {
        val publisher = mock(ApplicationEventPublisher::class.java)
        val service = AdminDiscordAlertService(publisher)

        service.sendPaymentSucceeded(
            orderId = "MCJ-10-test",
            pgPaymentId = "portone-payment-id",
            amount = 12000,
            method = "CARD",
        )

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.PAYMENT, captor.value.channel)
        assertTrue(captor.value.message.contains("결제 성공"))
        assertTrue(captor.value.message.contains("12,000원"))
        assertTrue(captor.value.message.contains("CARD"))
    }

    @Test
    fun `새 사장님 가입 알림을 보낼 때 온보딩 채널로 전송되고 입점 검토 문구가 포함됨`() {
        val publisher = mock(ApplicationEventPublisher::class.java)
        val service = AdminDiscordAlertService(publisher)
        val profile = SellerBusinessProfile(
            user = UserFixture.createKakaoUser(id = 99L),
            businessRegistrationNumber = "1234567890",
            storeName = "몽치장베이커리",
            ownerName = "이준교",
            storeAddress = "서울 성동구",
            phoneNumber = "01012345678",
        )

        service.sendNewSellerSignup(profile)

        val captor = ArgumentCaptor.forClass(AdminDiscordAlertRequestedEvent::class.java)
        verify(publisher).publishEvent(captor.capture())
        assertEquals(AdminDiscordChannel.ONBOARDING, captor.value.channel)
        assertTrue(captor.value.message.contains("[신규] 새 사장님 가입"))
        assertTrue(captor.value.message.contains("몽치장베이커리 / 이준교"))
        assertTrue(captor.value.message.contains("→ 입점 검토 필요"))
    }
}
