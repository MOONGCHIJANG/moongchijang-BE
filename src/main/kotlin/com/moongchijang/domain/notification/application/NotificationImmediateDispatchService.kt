package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.event.NotificationImmediateTriggerEvent
import com.moongchijang.domain.notification.domain.entity.Notification
import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType
import com.moongchijang.domain.notification.domain.entity.NotificationDispatchHistory
import com.moongchijang.domain.notification.domain.entity.NotificationDispatchStatus
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.domain.entity.NotificationType
import com.moongchijang.domain.notification.domain.repository.NotificationDispatchHistoryRepository
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationImmediateDispatchService(
    private val notificationRepository: NotificationRepository,
    private val notificationDispatchHistoryRepository: NotificationDispatchHistoryRepository,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun dispatch(event: NotificationImmediateTriggerEvent) {
        val dedupedUserIds = event.userIds.distinct()
        if (dedupedUserIds.isEmpty()) {
            return
        }

        dedupedUserIds.forEach { userId ->
            val exists = notificationDispatchHistoryRepository.findByUserIdAndTriggerTypeAndTargetIdAndScheduleKey(
                userId = userId,
                triggerType = event.triggerType,
                targetId = event.targetId,
                scheduleKey = event.scheduleKey
            ).isPresent
            if (exists) {
                log.info(
                    "[NotificationImmediateDispatchService] 즉시 발송 중복 스킵: userId={}, triggerType={}, targetId={}, scheduleKey={}",
                    userId, event.triggerType, event.targetId, event.scheduleKey
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

            try {
                val user = userRepository.findByIdAndDeletedAtIsNull(userId)
                if (user == null) {
                    history.markFailed(errorMessage = "USER_NOT_FOUND", nextRetryAt = null)
                    return@forEach
                }

                val meta = toNotificationMeta(event.triggerType)
                notificationRepository.save(
                    Notification(
                        user = user,
                        type = meta.type,
                        title = meta.title,
                        body = meta.body,
                        occurredAt = event.occurredAt,
                        targetId = event.targetId,
                        deeplinkType = meta.deeplinkType
                    )
                )
                history.markSuccess(event.occurredAt)
            } catch (e: Exception) {
                history.markFailed(
                    errorMessage = e.message ?: "UNKNOWN_ERROR",
                    nextRetryAt = null
                )
                log.error(
                    "[NotificationImmediateDispatchService] 즉시 발송 실패: userId={}, triggerType={}, targetId={}, scheduleKey={}",
                    userId, event.triggerType, event.targetId, event.scheduleKey, e
                )
            }
        }
    }

    private fun toNotificationMeta(triggerType: NotificationTriggerType): NotificationMeta {
        val notificationType = toNotificationType(triggerType)
        return NotificationMeta(
            type = notificationType,
            // TODO: 상세 템플릿/문구는 정해진 이후 수정 예정
            title = "알림",
            body = "새 알림이 도착했습니다.",
            deeplinkType = defaultDeeplinkType(notificationType)
        )
    }

    private fun toNotificationType(triggerType: NotificationTriggerType): NotificationType {
        return when (triggerType) {
            NotificationTriggerType.PICKUP_COMPLETED_IMMEDIATE,
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

    private fun defaultDeeplinkType(notificationType: NotificationType): NotificationDeeplinkType {
        return when (notificationType) {
            NotificationType.PICKUP -> NotificationDeeplinkType.PICKUP_GUIDE
            NotificationType.WISH -> NotificationDeeplinkType.GROUPBUY_DETAIL
            NotificationType.APPLY -> NotificationDeeplinkType.MY_APPLYING
            NotificationType.REQUEST -> NotificationDeeplinkType.REQUEST_STATUS
        }
    }

    private data class NotificationMeta(
        val type: NotificationType,
        val title: String,
        val body: String,
        val deeplinkType: NotificationDeeplinkType,
    )
}
