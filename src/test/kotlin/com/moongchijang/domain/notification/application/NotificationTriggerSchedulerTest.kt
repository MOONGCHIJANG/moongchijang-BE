package com.moongchijang.domain.notification.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.favorite.domain.repository.FavoriteNotificationTargetProjection
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.ParticipationFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class NotificationTriggerSchedulerTest {

    @Mock
    private lateinit var notificationEventPublisher: NotificationEventPublisher

    @Mock
    private lateinit var notificationImmediateDispatchService: NotificationImmediateDispatchService

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var favoriteRepository: FavoriteRepository

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    private val scheduler by lazy {
        NotificationTriggerScheduler(
            notificationEventPublisher = notificationEventPublisher,
            notificationImmediateDispatchService = notificationImmediateDispatchService,
            participationRepository = participationRepository,
            groupBuyRepository = groupBuyRepository,
            favoriteRepository = favoriteRepository,
            groupBuyRequestRepository = groupBuyRequestRepository
        )
    }

    @Test
    fun `오전 스케줄러를 실행할 때 픽업 당일 전일 알림 발행`() {
        val now = LocalDateTime.of(2026, 5, 23, 7, 0)
        val todayParticipation = ParticipationFixture.createParticipation(
            participationId = 1L,
            groupBuyId = 11L,
            quantity = 1,
            totalAmount = 1000,
            currentQuantity = 10,
            targetQuantity = 20,
            deadline = now.plusDays(1),
            pickupDate = now.toLocalDate(),
            pickupTimeStart = LocalTime.of(12, 0),
            createdAt = now.minusDays(1),
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatus = PickupStatus.NOT_READY
        )
        val tomorrowParticipation = ParticipationFixture.createParticipation(
            participationId = 2L,
            groupBuyId = 12L,
            quantity = 1,
            totalAmount = 1000,
            currentQuantity = 10,
            targetQuantity = 20,
            deadline = now.plusDays(2),
            pickupDate = now.toLocalDate().plusDays(1),
            pickupTimeStart = LocalTime.of(12, 0),
            createdAt = now.minusDays(1),
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatus = PickupStatus.NOT_READY
        )

        `when`(
            participationRepository.findForPickupReminderByPickupDate(
                now.toLocalDate(),
                listOf(ParticipationStatus.CONFIRMED),
                listOf(PickupStatus.NOT_READY, PickupStatus.READY)
            )
        ).thenReturn(listOf(todayParticipation))
        `when`(
            participationRepository.findForPickupReminderByPickupDate(
                now.toLocalDate().plusDays(1),
                listOf(ParticipationStatus.CONFIRMED),
                listOf(PickupStatus.NOT_READY, PickupStatus.READY)
            )
        ).thenReturn(listOf(tomorrowParticipation))

        scheduler.triggerPickupMorningNotificationsAt(now)

        verify(notificationEventPublisher).publishScheduledTrigger(
            NotificationTriggerType.PICKUP_SAME_DAY_MORNING,
            1L,
            listOf(1L),
            "pickup-same-day-morning:1:${now.toLocalDate()}",
            now
        )
        verify(notificationEventPublisher).publishScheduledTrigger(
            NotificationTriggerType.PICKUP_DAY_BEFORE_MORNING,
            2L,
            listOf(1L),
            "pickup-day-before-morning:2:${now.toLocalDate().plusDays(1)}",
            now
        )
    }

    @Test
    fun `찜 마감 리마인드 스케줄러를 실행할 때 D3 D1 알림 발행`() {
        val now = LocalDateTime.of(2026, 5, 23, 10, 0)
        val d3GroupBuy = GroupBuyFixture.createGroupBuy(
            id = 21L,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = now.plusHours(72).plusMinutes(1)
        )
        val d1GroupBuy = GroupBuyFixture.createGroupBuy(
            id = 22L,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = now.plusHours(24).plusMinutes(1)
        )

        `when`(
            groupBuyRepository.findByStatusInAndDeadlineBetween(
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                now.plusHours(72),
                now.plusHours(72).plusMinutes(10)
            )
        ).thenReturn(listOf(d3GroupBuy))
        `when`(
            groupBuyRepository.findByStatusInAndDeadlineBetween(
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                now.plusHours(24),
                now.plusHours(24).plusMinutes(10)
            )
        ).thenReturn(listOf(d1GroupBuy))
        `when`(
            favoriteRepository.findNotificationTargetsByGroupBuyIdsExcludingParticipants(listOf(21L))
        ).thenReturn(
            listOf(notificationTarget(groupBuyId = 21L, userId = 100L))
        )
        `when`(
            favoriteRepository.findNotificationTargetsByGroupBuyIdsExcludingParticipants(listOf(22L))
        ).thenReturn(
            listOf(notificationTarget(groupBuyId = 22L, userId = 200L))
        )

        scheduler.triggerWishlistDeadlineNotificationsAt(now)

        verify(notificationEventPublisher).publishScheduledTrigger(
            NotificationTriggerType.WISH_DEADLINE_MINUS_3_DAYS,
            21L,
            listOf(100L),
            "wish-deadline-d3:21:${d3GroupBuy.deadline}",
            now
        )
        verify(notificationEventPublisher).publishScheduledTrigger(
            NotificationTriggerType.WISH_DEADLINE_MINUS_1_DAY,
            22L,
            listOf(200L),
            "wish-deadline-d1:22:${d1GroupBuy.deadline}",
            now
        )
    }

    @Test
    fun `요청공구 D3 스케줄러를 실행할 때 요청자 대상 알림 발행`() {
        val now = LocalDateTime.of(2026, 5, 23, 7, 0)
        val request = GroupBuyRequest(
            userId = 7L,
            storeName = "테스트 매장",
            storeAddress = "서울",
            productName = "소금빵",
            desiredQuantity = 10,
            desiredPickupDate = now.toLocalDate().plusDays(3)
        ).apply { id = 31L }

        `when`(
            groupBuyRequestRepository.findByStatusInAndDesiredPickupDate(
                listOf(GroupBuyRequestStatus.IN_REVIEW, GroupBuyRequestStatus.IN_CONTACT),
                now.toLocalDate().plusDays(3)
            )
        ).thenReturn(listOf(request))

        scheduler.triggerRequestDeadlineMinus3DaysNotificationAt(now)

        verify(notificationEventPublisher).publishScheduledTrigger(
            NotificationTriggerType.REQUEST_DEADLINE_MINUS_3_DAYS,
            31L,
            listOf(7L),
            "request-deadline-d3:31:${now.toLocalDate().plusDays(3)}",
            now
        )
    }

    @Test
    fun `픽업 미완료 컷오프 스케줄러를 실행할 때 마감 후 30분 지난 참여 알림 발행`() {
        val now = LocalDateTime.of(2026, 5, 23, 10, 0)
        val participation = ParticipationFixture.createParticipation(
            participationId = 41L,
            groupBuyId = 41L,
            quantity = 1,
            totalAmount = 1000,
            currentQuantity = 10,
            targetQuantity = 20,
            deadline = now.minusDays(1),
            pickupDate = now.toLocalDate().minusDays(1),
            pickupTimeStart = LocalTime.of(8, 0),
            createdAt = now.minusDays(2),
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatus = PickupStatus.READY
        ).apply {
            groupBuy.pickupTimeEnd = LocalTime.of(9, 0)
        }

        `when`(
            participationRepository.findForPickupCutoffCheck(
                now.minusMinutes(30),
                listOf(ParticipationStatus.CONFIRMED),
                listOf(PickupStatus.NOT_READY, PickupStatus.READY)
            )
        ).thenReturn(listOf(participation))

        scheduler.triggerPickupIncompleteAfterCutoffNotificationAt(now)

        verify(notificationEventPublisher).publishScheduledTrigger(
            NotificationTriggerType.PICKUP_NOT_COMPLETED_AFTER_CUTOFF,
            41L,
            listOf(1L),
            "pickup-cutoff:41:${LocalDateTime.of(now.toLocalDate().minusDays(1), LocalTime.of(9, 0)).plusMinutes(30)}",
            now
        )
    }
}

private fun notificationTarget(groupBuyId: Long, userId: Long): FavoriteNotificationTargetProjection {
    return object : FavoriteNotificationTargetProjection {
        override val groupBuyId: Long = groupBuyId
        override val userId: Long = userId
    }
}
