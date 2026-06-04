package com.moongchijang.domain.participation.application

import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.support.ParticipationFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ParticipationPickupCommandServiceTest {

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-23T03:00:00Z"), ZoneOffset.UTC)

    private val service by lazy {
        ParticipationPickupCommandService(
            participationRepository = participationRepository,
            userRepository = userRepository,
            clock = clock
        )
    }

    @Test
    fun `픽업 완료를 처리할 때 픽업 상태 변경`() {
        val now = LocalDateTime.of(2026, 5, 23, 12, 0)
        val participation = ParticipationFixture.createParticipation(
            participationId = 101L,
            groupBuyId = 201L,
            quantity = 1,
            totalAmount = 1000,
            currentQuantity = 10,
            targetQuantity = 20,
            deadline = now.plusDays(1),
            pickupDate = LocalDate.of(2026, 5, 23),
            pickupTimeStart = LocalTime.of(10, 0),
            createdAt = now.minusDays(1)
        )
        val processor = UserFixture.createEmailUser(id = 999L)

        `when`(participationRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(participation))
        `when`(userRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(processor)

        service.completePickup(participationId = 101L, processedByUserId = 999L, pickedUpAt = now)

        assertEquals(PickupStatus.PICKED_UP, participation.pickupStatus)
        assertEquals(now, participation.pickedUpAt)
        assertEquals(999L, participation.pickupProcessedBy?.id)
    }
}
