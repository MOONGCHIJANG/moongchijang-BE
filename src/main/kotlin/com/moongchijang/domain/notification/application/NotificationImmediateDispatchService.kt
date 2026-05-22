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
        return when (triggerType) {
            NotificationTriggerType.WISH_TARGET_ACHIEVED_IMMEDIATE -> NotificationMeta(
                type = NotificationType.WISH,
                title = "찜한 공구가 달성됐어요",
                body = "공구가 목표 인원을 달성했습니다.",
                deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL
            )
            NotificationTriggerType.APPLY_PAYMENT_SUCCESS_IMMEDIATE -> NotificationMeta(
                type = NotificationType.APPLY,
                title = "공구 참여가 완료됐어요",
                body = "결제가 성공적으로 완료되었습니다.",
                deeplinkType = NotificationDeeplinkType.MY_APPLYING
            )
            NotificationTriggerType.APPLY_GROUPBUY_ACHIEVED_IMMEDIATE -> NotificationMeta(
                type = NotificationType.APPLY,
                title = "참여한 공구가 달성됐어요",
                body = "공구가 성공적으로 달성되었습니다.",
                deeplinkType = NotificationDeeplinkType.MY_APPLYING
            )
            NotificationTriggerType.APPLY_GROUPBUY_FAILED_IMMEDIATE -> NotificationMeta(
                type = NotificationType.APPLY,
                title = "참여한 공구가 미달성 마감됐어요",
                body = "미달성으로 마감되어 환불이 진행될 예정입니다.",
                deeplinkType = NotificationDeeplinkType.MY_APPLYING
            )
            NotificationTriggerType.PICKUP_COMPLETED_IMMEDIATE,
            NotificationTriggerType.PICKUP_SAME_DAY_MORNING,
            NotificationTriggerType.PICKUP_DAY_BEFORE_MORNING,
            NotificationTriggerType.PICKUP_NOT_COMPLETED_AFTER_CUTOFF -> NotificationMeta(
                type = NotificationType.PICKUP,
                title = "픽업 알림",
                body = "픽업 상태를 확인해주세요.",
                deeplinkType = NotificationDeeplinkType.PICKUP_GUIDE
            )
            NotificationTriggerType.WISH_DEADLINE_MINUS_3_DAYS,
            NotificationTriggerType.WISH_DEADLINE_MINUS_1_DAY -> NotificationMeta(
                type = NotificationType.WISH,
                title = "찜한 공구 알림",
                body = "찜한 공구의 마감일이 다가오고 있어요.",
                deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL
            )
            NotificationTriggerType.REQUEST_OPENED_IMMEDIATE,
            NotificationTriggerType.REQUEST_REJECTED_IMMEDIATE,
            NotificationTriggerType.REQUEST_NEW_PARTICIPANT_IMMEDIATE,
            NotificationTriggerType.REQUEST_TARGET_ACHIEVED_IMMEDIATE,
            NotificationTriggerType.REQUEST_DEADLINE_MINUS_3_DAYS -> NotificationMeta(
                type = NotificationType.REQUEST,
                title = "요청 공구 알림",
                body = "요청 공구 상태를 확인해주세요.",
                deeplinkType = NotificationDeeplinkType.REQUEST_STATUS
            )
        }
    }

    private data class NotificationMeta(
        val type: NotificationType,
        val title: String,
        val body: String,
        val deeplinkType: NotificationDeeplinkType,
    )
}
