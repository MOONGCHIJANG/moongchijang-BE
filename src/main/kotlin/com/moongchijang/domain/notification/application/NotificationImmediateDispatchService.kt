package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.event.NotificationImmediateTriggerEvent
import com.moongchijang.domain.notification.application.template.NotificationTemplateRegistry
import com.moongchijang.domain.notification.application.template.NotificationTemplateRenderer
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.notification.domain.entity.Notification
import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType
import com.moongchijang.domain.notification.domain.entity.NotificationDispatchHistory
import com.moongchijang.domain.notification.domain.entity.NotificationDispatchStatus
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.domain.entity.NotificationType
import com.moongchijang.domain.notification.domain.repository.NotificationDispatchHistoryRepository
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

@Service
class NotificationImmediateDispatchService(
    private val notificationRepository: NotificationRepository,
    private val notificationDispatchHistoryRepository: NotificationDispatchHistoryRepository,
    private val userRepository: UserRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val notificationTemplateRegistry: NotificationTemplateRegistry,
    private val notificationTemplateRenderer: NotificationTemplateRenderer,
) {
    companion object {
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val zoneId: ZoneId = ZoneId.of("Asia/Seoul")

    @Transactional
    fun dispatch(event: NotificationImmediateTriggerEvent) {
        val dedupedUserIds = event.userIds.distinct()
        if (dedupedUserIds.isEmpty()) {
            return
        }

        dedupedUserIds.forEach { userId ->
            val existingHistory = notificationDispatchHistoryRepository.findByUserIdAndTriggerTypeAndTargetIdAndScheduleKey(
                userId = userId,
                triggerType = event.triggerType,
                targetId = event.targetId,
                scheduleKey = event.scheduleKey
            ).orElse(null)
            if (existingHistory != null) {
                log.info(
                    "[NotificationImmediateDispatchService] 즉시 발송 중복 스킵: userId={}, triggerType={}, targetId={}, scheduleKey={}, retryCount={}",
                    userId, event.triggerType, event.targetId, event.scheduleKey, existingHistory.retryCount
                )
                return@forEach
            }

            val history = notificationDispatchHistoryRepository.save(
                NotificationDispatchHistory(
                    userId = userId,
                    triggerType = event.triggerType,
                    targetId = event.targetId,
                    scheduleKey = event.scheduleKey,
                    status = NotificationDispatchStatus.PENDING
                )
            )

            dispatchSingle(event, userId, history)
        }
    }

    @Transactional
    fun retryFailedDispatches(now: LocalDateTime) {
        val targets = notificationDispatchHistoryRepository.findByStatusInAndNextRetryAtLessThanEqual(
            statuses = listOf(NotificationDispatchStatus.FAILED),
            nextRetryAt = now
        )
        if (targets.isEmpty()) {
            return
        }

        targets.forEach { history ->
            dispatchSingle(
                event = NotificationImmediateTriggerEvent(
                    triggerType = history.triggerType,
                    targetId = history.targetId,
                    userIds = listOf(history.userId),
                    scheduleKey = history.scheduleKey,
                    occurredAt = now
                ),
                userId = history.userId,
                history = history
            )
        }
    }

    private fun toNotificationMeta(triggerType: NotificationTriggerType, targetId: Long): NotificationMeta {
        val notificationType = toNotificationType(triggerType)
        val template = notificationTemplateRegistry.getTemplateByTriggerType(triggerType)
        val rendered = notificationTemplateRenderer.render(
            template = template,
            variables = resolveTemplateVariables(triggerType, targetId)
        )
        return NotificationMeta(
            type = notificationType,
            title = rendered.title,
            body = rendered.body,
            deeplinkType = rendered.deeplinkType
        )
    }

    private fun toNotificationType(triggerType: NotificationTriggerType): NotificationType {
        return when (triggerType) {
            NotificationTriggerType.PICKUP_SAME_DAY_MORNING,
            NotificationTriggerType.PICKUP_DAY_BEFORE_MORNING,
            NotificationTriggerType.PICKUP_NOT_COMPLETED_AFTER_CUTOFF -> NotificationType.PICKUP

            NotificationTriggerType.WISH_DEADLINE_MINUS_3_DAYS,
            NotificationTriggerType.WISH_DEADLINE_MINUS_1_DAY,
            NotificationTriggerType.WISH_TARGET_ACHIEVED_IMMEDIATE -> NotificationType.WISH

            NotificationTriggerType.APPLY_PAYMENT_SUCCESS_IMMEDIATE,
            NotificationTriggerType.APPLY_GROUPBUY_ACHIEVED_IMMEDIATE,
            NotificationTriggerType.APPLY_GROUPBUY_FAILED_IMMEDIATE -> NotificationType.APPLY

            NotificationTriggerType.REQUEST_OPENED_IMMEDIATE,
            NotificationTriggerType.REQUEST_REJECTED_IMMEDIATE,
            NotificationTriggerType.REQUEST_NEW_PARTICIPANT_IMMEDIATE,
            NotificationTriggerType.REQUEST_TARGET_ACHIEVED_IMMEDIATE,
            NotificationTriggerType.REQUEST_DEADLINE_MINUS_3_DAYS -> NotificationType.REQUEST
        }
    }

    private data class NotificationMeta(
        val type: NotificationType,
        val title: String,
        val body: String,
        val deeplinkType: NotificationDeeplinkType,
    )

    private fun resolveTemplateVariables(triggerType: NotificationTriggerType, targetId: Long): Map<String, String> {
        val variables = mutableMapOf<String, String>()

        when (triggerType) {
            NotificationTriggerType.REQUEST_OPENED_IMMEDIATE,
            NotificationTriggerType.REQUEST_REJECTED_IMMEDIATE,
            NotificationTriggerType.REQUEST_NEW_PARTICIPANT_IMMEDIATE,
            NotificationTriggerType.REQUEST_TARGET_ACHIEVED_IMMEDIATE,
            NotificationTriggerType.REQUEST_DEADLINE_MINUS_3_DAYS -> {
                val groupBuyRequest = groupBuyRequestRepository.findById(targetId)
                    .orElseThrow {
                        CustomException(
                            ErrorCode.NOTIFICATION_NOT_FOUND,
                            "알림 템플릿 대상 요청공구를 찾을 수 없습니다. targetId=$targetId, triggerType=$triggerType"
                        )
                    }

                variables["상품명"] = groupBuyRequest.productName
                variables["목표참여개수"] = groupBuyRequest.desiredQuantity.toString()
                groupBuyRequest.openedGroupBuyId?.let { openedGroupBuyId ->
                    groupBuyRepository.findWithStoreById(openedGroupBuyId).ifPresent { groupBuy ->
                        variables["현재참여개수"] = groupBuy.currentQuantity.toString()
                    }
                }
            }

            else -> {
                val groupBuy = groupBuyRepository.findWithStoreById(targetId)
                    .orElseThrow {
                        CustomException(
                            ErrorCode.NOTIFICATION_NOT_FOUND,
                            "알림 템플릿 대상 공구를 찾을 수 없습니다. targetId=$targetId, triggerType=$triggerType"
                        )
                    }

                variables["상품명"] = groupBuy.productName
                variables["픽업시간범위"] =
                    "${groupBuy.pickupTimeStart.format(TIME_FORMATTER)} ~ ${groupBuy.pickupTimeEnd.format(TIME_FORMATTER)}"
                variables["픽업일자"] = groupBuy.pickupDate.format(DATE_FORMATTER)
                variables["매장명"] = groupBuy.store.name
                variables["매장주소"] = groupBuy.store.address
                variables["마감시각"] = groupBuy.deadline.format(TIME_FORMATTER)
                variables["목표참여개수"] = groupBuy.targetQuantity.toString()
                variables["현재참여개수"] = groupBuy.currentQuantity.toString()
                variables["환불예상시각"] = estimateRefundDateTime(groupBuy.deadline).format(DATETIME_FORMATTER)
            }
        }

        return variables
    }

    private fun estimateRefundDateTime(baseDateTime: LocalDateTime): LocalDateTime {
        var cursor = baseDateTime
        var addedBusinessDays = 0
        while (addedBusinessDays < 3) {
            cursor = cursor.plusDays(1)
            if (cursor.dayOfWeek != DayOfWeek.SATURDAY && cursor.dayOfWeek != DayOfWeek.SUNDAY) {
                addedBusinessDays += 1
            }
        }
        return cursor.withHour(10).withMinute(0).withSecond(0).withNano(0)
    }

    private fun calculateNextRetryAt(retryCount: Int): LocalDateTime? {
        val now = LocalDateTime.now(zoneId)
        return when {
            retryCount <= 1 -> now.plusMinutes(1)
            retryCount == 2 -> now.plusMinutes(5)
            retryCount == 3 -> now.plusMinutes(30)
            else -> null
        }
    }

    private fun dispatchSingle(
        event: NotificationImmediateTriggerEvent,
        userId: Long,
        history: NotificationDispatchHistory
    ) {
        try {
            val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            if (user == null) {
                history.markFailed(
                    errorMessage = "USER_NOT_FOUND",
                    nextRetryAt = calculateNextRetryAt(history.retryCount)
                )
                log.warn(
                    "[NotificationImmediateDispatchService] 즉시 발송 실패(USER_NOT_FOUND): userId={}, triggerType={}, targetId={}, scheduleKey={}, retryCount={}, nextRetryAt={}",
                    userId, event.triggerType, event.targetId, event.scheduleKey, history.retryCount, history.nextRetryAt
                )
                return
            }

            val meta = toNotificationMeta(event.triggerType, event.targetId)
            notificationRepository.save(
                Notification(
                    user = user,
                    type = meta.type,
                    title = meta.title,
                    body = meta.body,
                    occurredAt = event.occurredAt,
                    targetId = event.targetId,
                    deeplinkType = meta.deeplinkType,
                    triggerType = event.triggerType
                )
            )
            history.markSuccess(event.occurredAt)
            log.info(
                "[NotificationImmediateDispatchService] 즉시 발송 성공: userId={}, triggerType={}, targetId={}, scheduleKey={}, retryCount={}",
                userId, event.triggerType, event.targetId, event.scheduleKey, history.retryCount
            )
        } catch (e: Exception) {
            history.markFailed(
                errorMessage = e.message ?: "UNKNOWN_ERROR",
                nextRetryAt = calculateNextRetryAt(history.retryCount)
            )
            log.error(
                "[NotificationImmediateDispatchService] 즉시 발송 실패: userId={}, triggerType={}, targetId={}, scheduleKey={}, retryCount={}, nextRetryAt={}",
                userId, event.triggerType, event.targetId, event.scheduleKey, history.retryCount, history.nextRetryAt, e
            )
        }
    }
}
