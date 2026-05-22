package com.moongchijang.domain.notification.application

import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Component
class NotificationTriggerScheduler(
    private val notificationEventPublisher: NotificationEventPublisher,
    private val participationRepository: ParticipationRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val favoriteRepository: FavoriteRepository,
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 7 * * *", zone = KST_ZONE_ID)
    fun triggerPickupMorningNotifications() {
        val now = nowKst()
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        dispatchPickupReminder(
            pickupDate = today,
            triggerType = NotificationTriggerType.PICKUP_SAME_DAY_MORNING,
            scheduleKeyPrefix = "pickup-same-day-morning",
            occurredAt = now
        )
        dispatchPickupReminder(
            pickupDate = tomorrow,
            triggerType = NotificationTriggerType.PICKUP_DAY_BEFORE_MORNING,
            scheduleKeyPrefix = "pickup-day-before-morning",
            occurredAt = now
        )
    }

    @Scheduled(cron = "0 */10 * * * *", zone = KST_ZONE_ID)
    fun triggerWishlistDeadlineNotifications() {
        val now = nowKst()
        dispatchWishlistDeadlineReminder(now, ReminderOffset.DAYS_3)
        dispatchWishlistDeadlineReminder(now, ReminderOffset.DAYS_1)
    }

    @Scheduled(cron = "0 0 7 * * *", zone = KST_ZONE_ID)
    fun triggerRequestDeadlineMinus3DaysNotification() {
        val now = nowKst()
        val targetDate = now.toLocalDate().plusDays(3)
        val requests = groupBuyRequestRepository.findByStatusInAndDesiredPickupDate(
            statuses = listOf(GroupBuyRequestStatus.IN_REVIEW, GroupBuyRequestStatus.IN_CONTACT),
            desiredPickupDate = targetDate
        )

        requests.forEach { request ->
            notificationEventPublisher.publishScheduledTrigger(
                triggerType = NotificationTriggerType.REQUEST_DEADLINE_MINUS_3_DAYS,
                targetId = request.id,
                userIds = listOf(request.userId),
                scheduleKey = "request-deadline-d3:${request.id}:$targetDate",
                occurredAt = now
            )
        }
        log.info(
            "[NotificationTriggerScheduler] 요청공구 D-3 알림 트리거 완료: targetDate={}, requestCount={}",
            targetDate, requests.size
        )
    }

    @Scheduled(cron = "0 */10 * * * *", zone = KST_ZONE_ID)
    fun triggerPickupIncompleteAfterCutoffNotification() {
        val now = nowKst()
        val candidates = participationRepository.findForPickupCutoffCheck(
            pickupDateFrom = now.toLocalDate().minusDays(1),
            pickupDateTo = now.toLocalDate(),
            participationStatuses = listOf(ParticipationStatus.CONFIRMED),
            pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
        )

        var triggered = 0
        candidates.forEach { participation ->
            val cutoffAt = LocalDateTime.of(
                participation.groupBuy.pickupDate,
                participation.groupBuy.pickupTimeEnd
            ).plusMinutes(30)
            if (now >= cutoffAt) {
                notificationEventPublisher.publishScheduledTrigger(
                    triggerType = NotificationTriggerType.PICKUP_NOT_COMPLETED_AFTER_CUTOFF,
                    targetId = participation.groupBuy.id,
                    userIds = listOf(participation.user.id!!),
                    scheduleKey = "pickup-cutoff:${participation.id}:${cutoffAt}",
                    occurredAt = now
                )
                triggered += 1
            }
        }

        log.info(
            "[NotificationTriggerScheduler] 픽업 미완료 컷오프 알림 트리거 완료: candidateCount={}, triggered={}",
            candidates.size, triggered
        )
    }

    private fun dispatchPickupReminder(
        pickupDate: LocalDate,
        triggerType: NotificationTriggerType,
        scheduleKeyPrefix: String,
        occurredAt: LocalDateTime
    ) {
        val participations = participationRepository.findForPickupReminderByPickupDate(
            pickupDate = pickupDate,
            participationStatuses = listOf(ParticipationStatus.CONFIRMED),
            pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY)
        )

        participations.groupBy { it.groupBuy.id }.forEach { (groupBuyId, items) ->
            val userIds = items.mapNotNull { it.user.id }.distinct()
            if (userIds.isEmpty()) return@forEach
            notificationEventPublisher.publishScheduledTrigger(
                triggerType = triggerType,
                targetId = groupBuyId,
                userIds = userIds,
                scheduleKey = "$scheduleKeyPrefix:$groupBuyId:$pickupDate",
                occurredAt = occurredAt
            )
        }
        log.info(
            "[NotificationTriggerScheduler] 픽업 리마인드 알림 트리거 완료: triggerType={}, pickupDate={}, participationCount={}",
            triggerType, pickupDate, participations.size
        )
    }

    private fun dispatchWishlistDeadlineReminder(now: LocalDateTime, offset: ReminderOffset) {
        val windowFrom = now.plusHours(offset.hours)
        val windowTo = windowFrom.plusMinutes(WINDOW_MINUTES.toLong())

        val groupBuys = groupBuyRepository.findByStatusInAndDeadlineBetween(
            statuses = listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
            deadlineFrom = windowFrom,
            deadlineTo = windowTo
        )

        groupBuys.forEach { groupBuy ->
            val userIds = favoriteRepository.findUserIdsByGroupBuyIdExcludingParticipants(groupBuy.id)
            if (userIds.isEmpty()) return@forEach

            notificationEventPublisher.publishScheduledTrigger(
                triggerType = offset.triggerType,
                targetId = groupBuy.id,
                userIds = userIds,
                scheduleKey = "${offset.scheduleKeyPrefix}:${groupBuy.id}:${groupBuy.deadline}",
                occurredAt = now
            )
        }

        log.info(
            "[NotificationTriggerScheduler] 찜 마감 리마인드 알림 트리거 완료: triggerType={}, windowFrom={}, windowTo={}, groupBuyCount={}",
            offset.triggerType, windowFrom, windowTo, groupBuys.size
        )
    }

    private fun nowKst(): LocalDateTime = LocalDateTime.now(ZoneId.of(KST_ZONE_ID))

    private enum class ReminderOffset(
        val hours: Long,
        val triggerType: NotificationTriggerType,
        val scheduleKeyPrefix: String
    ) {
        DAYS_3(72, NotificationTriggerType.WISH_DEADLINE_MINUS_3_DAYS, "wish-deadline-d3"),
        DAYS_1(24, NotificationTriggerType.WISH_DEADLINE_MINUS_1_DAY, "wish-deadline-d1"),
    }

    companion object {
        private const val KST_ZONE_ID = "Asia/Seoul"
        private const val WINDOW_MINUTES = 10
    }
}
