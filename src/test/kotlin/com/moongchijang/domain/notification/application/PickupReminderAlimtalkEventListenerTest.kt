package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.event.NotificationImmediateTriggerEvent
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.infrastructure.aligo.AligoAlimtalkClient
import com.moongchijang.domain.notification.infrastructure.aligo.AligoMessageFormatter
import com.moongchijang.domain.notification.infrastructure.aligo.AligoProperties
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.support.ParticipationFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class PickupReminderAlimtalkEventListenerTest {

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var aligoAlimtalkClient: AligoAlimtalkClient

    @Mock
    private lateinit var aligoProperties: AligoProperties

    private val listener by lazy {
        PickupReminderAlimtalkEventListener(
            participationRepository = participationRepository,
            aligoAlimtalkClient = aligoAlimtalkClient,
            aligoProperties = aligoProperties,
        )
    }

    @Test
    fun `수령일 D-1 이벤트 수신 시 알림톡을 발송한다`() {
        val now = LocalDateTime.of(2026, 5, 23, 7, 0)
        val participation = ParticipationFixture.createParticipation(
            participationId = 51L,
            groupBuyId = 51L,
            quantity = 1,
            totalAmount = 1000,
            currentQuantity = 10,
            targetQuantity = 20,
            deadline = now.plusDays(2),
            pickupDate = now.toLocalDate().plusDays(1),
            pickupTimeStart = LocalTime.of(12, 0),
            createdAt = now.minusDays(1),
        ).apply {
            user.phoneNumber = "01012345678"
        }

        `when`(participationRepository.findForPickupReminderById(51L)).thenReturn(participation)
        `when`(aligoProperties.templateCodePickupD1Reminder).thenReturn("UH_7967")

        listener.on(
            NotificationImmediateTriggerEvent(
                triggerType = NotificationTriggerType.PICKUP_DAY_BEFORE_MORNING,
                targetId = 51L,
                userIds = listOf(1L),
                scheduleKey = "pickup-day-before-morning:51:2026-05-24",
                occurredAt = now,
            )
        )

        val expectedMessage = AligoMessageFormatter.pickupD1Reminder(
            nickname = "테스터",
            productName = "두쫀쿠 오리지널 1개",
            pickupPlace = "서울 강남구 OO길 1",
            pickupDateTime = "2026.05.24 12:00 ~ 16:00",
        )
        verify(aligoAlimtalkClient).send("01012345678", expectedMessage, "UH_7967")
    }

    @Test
    fun `수령일 당일 이벤트 수신 시 알림톡을 발송한다`() {
        val now = LocalDateTime.of(2026, 5, 23, 7, 0)
        val participation = ParticipationFixture.createParticipation(
            participationId = 61L,
            groupBuyId = 61L,
            quantity = 1,
            totalAmount = 1000,
            currentQuantity = 10,
            targetQuantity = 20,
            deadline = now.plusDays(1),
            pickupDate = now.toLocalDate(),
            pickupTimeStart = LocalTime.of(12, 0),
            createdAt = now.minusDays(1),
        ).apply {
            user.phoneNumber = "01099998888"
        }

        `when`(participationRepository.findForPickupReminderById(61L)).thenReturn(participation)
        `when`(aligoProperties.templateCodePickupDayReminder).thenReturn("UH_7968")

        listener.on(
            NotificationImmediateTriggerEvent(
                triggerType = NotificationTriggerType.PICKUP_SAME_DAY_MORNING,
                targetId = 61L,
                userIds = listOf(1L),
                scheduleKey = "pickup-same-day-morning:61:2026-05-23",
                occurredAt = now,
            )
        )

        val expectedMessage = AligoMessageFormatter.pickupDayReminder(
            nickname = "테스터",
            productName = "두쫀쿠 오리지널 1개",
            pickupPlace = "서울 강남구 OO길 1",
            pickupDateTime = "2026.05.23 12:00 ~ 16:00",
        )
        verify(aligoAlimtalkClient).send("01099998888", expectedMessage, "UH_7968")
    }
}
