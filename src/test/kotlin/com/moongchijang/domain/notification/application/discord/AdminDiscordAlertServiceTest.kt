package com.moongchijang.domain.notification.application.discord

import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdminDiscordAlertServiceTest {

    @Test
    fun `새 공구 요청 알림을 보낼 때 온보딩 채널로 전송됨`() {
        val sender = FakeDiscordMessageSender()
        val service = AdminDiscordAlertService(sender)
        val request = GroupBuyFixture.createGroupBuyRequest(storeName = "몽치장베이커리", productName = "소금빵", desiredQuantity = 3)

        service.sendNewGroupBuyRequest(request)

        assertEquals(AdminDiscordChannel.ONBOARDING, sender.lastChannel)
        assertTrue(sender.lastMessage!!.contains("[새 요청]"))
    }

    @Test
    fun `공구 달성 알림을 보낼 때 공구 채널로 전송되고 금액 문구가 포함됨`() {
        val sender = FakeDiscordMessageSender()
        val service = AdminDiscordAlertService(sender)
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 11L,
            status = com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus.ACHIEVED,
            productName = "소금빵",
            price = 5000,
            currentQuantity = 20,
        )

        service.sendGroupBuyAchieved(groupBuy, participantCount = 18)

        assertEquals(AdminDiscordChannel.GROUPBUY, sender.lastChannel)
        assertTrue(sender.lastMessage!!.contains("총 100,000원"))
    }

    private class FakeDiscordMessageSender : DiscordMessageSender {
        var lastChannel: AdminDiscordChannel? = null
        var lastMessage: String? = null

        override fun send(channel: AdminDiscordChannel, message: String): Boolean {
            lastChannel = channel
            lastMessage = message
            return true
        }
    }
}
