package com.moongchijang.domain.notification.application.discord

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.user.domain.entity.SellerBusinessProfile
import org.springframework.stereotype.Service
import java.text.NumberFormat
import java.util.Locale

@Service
class AdminDiscordAlertService(
    private val discordMessageSender: DiscordMessageSender,
) {
    fun sendNewGroupBuyRequest(request: GroupBuyRequest) {
        val requester = request.user.nickname ?: "이름미입력"
        val message = """
            [새 요청] ${request.storeName} - ${request.productName}
            요청자: $requester / 희망수량: ${request.desiredQuantity}개
            희망날짜: ${request.desiredPickupDate}
            → 어드민 확인 필요
        """.trimIndent()
        discordMessageSender.send(AdminDiscordChannel.ONBOARDING, message)
    }

    fun sendGroupBuyAchieved(groupBuy: GroupBuy, participantCount: Int) {
        val totalAmount = groupBuy.currentQuantity * groupBuy.price
        val message = """
            [달성] ${groupBuy.store.name} - ${groupBuy.productName} 확정
            참여자 ${participantCount}명 / 총 ${toWon(totalAmount)}원
            픽업일: ${groupBuy.pickupDate}
            → 발주 확정 필요
        """.trimIndent()
        discordMessageSender.send(AdminDiscordChannel.GROUPBUY, message)
    }

    fun sendGroupBuyFailed(groupBuy: GroupBuy, participantCount: Int) {
        val message = """
            [미달성] ${groupBuy.store.name} - ${groupBuy.productName} 해산
            참여자 ${participantCount}명 자동 환불 처리 중
        """.trimIndent()
        discordMessageSender.send(AdminDiscordChannel.GROUPBUY, message)
    }

    fun sendRefundFailedSummary(failedCount: Int) {
        val message = """
            [긴급] 환불 실패 발생
            실패 건수: ${failedCount}건
            → 수동 처리 필요
        """.trimIndent()
        discordMessageSender.send(AdminDiscordChannel.REFUND, message)
    }

    fun sendNewSellerSignup(profile: SellerBusinessProfile) {
        val ownerName = profile.ownerName.ifBlank { "이름미입력" }
        val message = """
            [신규] 새 사장님 가입
            ${profile.storeName} / ${ownerName}
            → 입점 검토 필요
        """.trimIndent()
        discordMessageSender.send(AdminDiscordChannel.ONBOARDING, message)
    }

    private fun toWon(amount: Int): String =
        NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
}
