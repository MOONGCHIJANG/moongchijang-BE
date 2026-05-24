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
    private val notificationImmediateDispatchService: NotificationImmediateDispatchService,
    private val participationRepository: ParticipationRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val favoriteRepository: FavoriteRepository,
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 7 * * *", zone = KST_ZONE_ID)
    fun triggerPickupMorningNotifications() {
        triggerPickupMorningNotificationsAt(nowKst())
    }

    fun triggerPickupMorningNotificationsAt(now: LocalDateTime) {
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
        triggerWishlistDeadlineNotificationsAt(nowKst())
    }

    fun triggerWishlistDeadlineNotificationsAt(now: LocalDateTime) {
        dispatchWishlistDeadlineReminder(now, ReminderOffset.DAYS_3)
        dispatchWishlistDeadlineReminder(now, ReminderOffset.DAYS_1)
    }

    @Scheduled(cron = "0 0 7 * * *", zone = KST_ZONE_ID)
    fun triggerRequestDeadlineMinus3DaysNotification() {
        triggerRequestDeadlineMinus3DaysNotificationAt(nowKst())
    }

    fun triggerRequestDeadlineMinus3DaysNotificationAt(now: LocalDateTime) {
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
        triggerPickupIncompleteAfterCutoffNotificationAt(nowKst())
    }

    fun triggerPickupIncompleteAfterCutoffNotificationAt(now: LocalDateTime) {
        val pickupCutoffBaseAt = now.minusMinutes(30)
        val candidates = participationRepository.findForPickupCutoffCheck(
            pickupCutoffBaseAt = pickupCutoffBaseAt,
            participationStatuses = listOf(ParticipationStatus.CONFIRMED),
            pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
        )

        candidates.forEach { participation ->
            val cutoffAt = LocalDateTime.of(
                participation.groupBuy.pickupDate,
                participation.groupBuy.pickupTimeEnd
            ).plusMinutes(30)
            val userId = requireNotNull(participation.user.id) { "참여자 userId는 null일 수 없습니다." }
            notificationEventPublisher.publishScheduledTrigger(
                triggerType = NotificationTriggerType.PICKUP_NOT_COMPLETED_AFTER_CUTOFF,
                targetId = participation.id,
                userIds = listOf(userId),
                scheduleKey = "pickup-cutoff:${participation.id}:${cutoffAt}",
                occurredAt = now
            )
        }

        log.info(
            "[NotificationTriggerScheduler] 픽업 미완료 컷오프 알림 트리거 완료: candidateCount={}, triggered={}",
            candidates.size, candidates.size
        )
    }

    @Scheduled(cron = "0 */5 * * * *", zone = KST_ZONE_ID)
    fun retryFailedImmediateDispatches() {
        val now = nowKst()
        notificationImmediateDispatchService.retryFailedDispatches(now)
        log.info("[NotificationTriggerScheduler] 즉시 발송 실패 재처리 실행: now={}", now)
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

        participations.forEach { participation ->
            val userId = requireNotNull(participation.user.id) { "참여자 userId는 null일 수 없습니다." }
            notificationEventPublisher.publishScheduledTrigger(
                triggerType = triggerType,
                targetId = participation.id,
                userIds = listOf(userId),
                scheduleKey = "$scheduleKeyPrefix:${participation.id}:$pickupDate",
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

        val groupBuyIds = groupBuys.map { it.id }
        if (groupBuyIds.isEmpty()) {
            log.info(
                "[NotificationTriggerScheduler] 찜 마감 리마인드 알림 트리거 완료: triggerType={}, windowFrom={}, windowTo={}, groupBuyCount={}",
                offset.triggerType, windowFrom, windowTo, groupBuys.size
            )
            return
        }
        val targetsByGroupBuyId = favoriteRepository
            .findNotificationTargetsByGroupBuyIdsExcludingParticipants(groupBuyIds)
            .groupBy(keySelector = { it.groupBuyId }, valueTransform = { it.userId })

        groupBuys.forEach { groupBuy ->
            val userIds = targetsByGroupBuyId[groupBuy.id].orEmpty().distinct()
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
