package com.moongchijang.domain.mypage.application.dto

import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.support.ParticipationFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MypageParticipationResponseTest {

    @Test
    fun `KST 기준 내일 픽업 건은 QR이 잠겨 있고 D-day는 1이다`() {
        val today = LocalDate.of(2026, 1, 31)
        val participation = ParticipationFixture.createParticipation(
            participationId = 1L,
            groupBuyId = 101L,
            quantity = 1,
            totalAmount = 12000,
            currentQuantity = 10,
            targetQuantity = 20,
            deadline = LocalDateTime.of(2026, 2, 1, 0, 10),
            pickupDate = LocalDate.of(2026, 2, 1),
            pickupTimeStart = LocalTime.of(12, 0),
            createdAt = LocalDateTime.of(2026, 1, 30, 18, 0),
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatus = PickupStatus.NOT_READY,
        )

        val response = MypageParticipationResponse.from(
            participation = participation,
            thumbnailUrl = "https://example.com/image.jpg",
            today = today,
        )

        assertEquals(1, response.dDay)
        assertEquals("LOCKED", response.qrAvailability)
        assertEquals(true, response.canViewPickup)
        assertEquals(true, response.canViewQr)
    }

    @Test
    fun `KST 기준 당일 픽업 건은 QR 조회가 가능하고 D-day는 0이다`() {
        val today = LocalDate.of(2026, 2, 1)
        val participation = ParticipationFixture.createParticipation(
            participationId = 2L,
            groupBuyId = 102L,
            quantity = 1,
            totalAmount = 12000,
            currentQuantity = 10,
            targetQuantity = 20,
            deadline = LocalDateTime.of(2026, 2, 1, 0, 10),
            pickupDate = LocalDate.of(2026, 2, 1),
            pickupTimeStart = LocalTime.of(12, 0),
            createdAt = LocalDateTime.of(2026, 1, 31, 23, 30),
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatus = PickupStatus.NOT_READY,
        )

        val response = MypageParticipationResponse.from(
            participation = participation,
            thumbnailUrl = "https://example.com/image.jpg",
            today = today,
        )

        assertEquals(0, response.dDay)
        assertEquals("AVAILABLE", response.qrAvailability)
        assertEquals(true, response.canViewPickup)
        assertEquals(true, response.canViewQr)
    }
}
