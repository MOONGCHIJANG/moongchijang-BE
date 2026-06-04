package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.event.NotificationImmediateTriggerEvent
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.infrastructure.aligo.AligoAlimtalkClient
import com.moongchijang.domain.notification.infrastructure.aligo.AligoMessageFormatter
import com.moongchijang.domain.notification.infrastructure.aligo.AligoProperties
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.security.crypto.PersonalInfoManager
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class PickupReminderAlimtalkEventListener(
    private val participationRepository: ParticipationRepository,
    private val aligoAlimtalkClient: AligoAlimtalkClient,
    private val aligoProperties: AligoProperties,
    private val personalInfoManager: PersonalInfoManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("notificationEventExecutor")
    @EventListener
    fun on(event: NotificationImmediateTriggerEvent) {
        if (event.triggerType != NotificationTriggerType.PICKUP_SAME_DAY_MORNING &&
            event.triggerType != NotificationTriggerType.PICKUP_DAY_BEFORE_MORNING
        ) {
            return
        }

        runCatching {
            val participation = participationRepository.findForPickupReminderById(event.targetId)
                ?: return
            val receiverPhone = personalInfoManager.decryptIfNeeded(participation.user.phoneNumber)?.trim().orEmpty()
            if (receiverPhone.isBlank()) {
                log.warn(
                    "[PickupReminderAlimtalkEventListener] 픽업 리마인드 알림톡 스킵(전화번호 없음): participationId={}, userId={}, triggerType={}",
                    participation.id, participation.user.id, event.triggerType
                )
                return
            }

            val groupBuy = participation.groupBuy
            val pickupDateTime =
                "${groupBuy.pickupDate.format(PICKUP_DATE_FORMATTER)} ${groupBuy.pickupTimeStart.format(PICKUP_TIME_FORMATTER)} ~ ${groupBuy.pickupTimeEnd.format(PICKUP_TIME_FORMATTER)}"
            val message = if (event.triggerType == NotificationTriggerType.PICKUP_DAY_BEFORE_MORNING) {
                AligoMessageFormatter.pickupD1Reminder(
                    nickname = participation.user.nickname ?: "고객",
                    productName = groupBuy.productName,
                    pickupPlace = groupBuy.store.address,
                    pickupDateTime = pickupDateTime,
                )
            } else {
                AligoMessageFormatter.pickupDayReminder(
                    nickname = participation.user.nickname ?: "고객",
                    productName = groupBuy.productName,
                    pickupPlace = groupBuy.store.address,
                    pickupDateTime = pickupDateTime,
                )
            }

            val templateCode = if (event.triggerType == NotificationTriggerType.PICKUP_DAY_BEFORE_MORNING) {
                aligoProperties.templateCodePickupD1Reminder
            } else {
                aligoProperties.templateCodePickupDayReminder
            }

            aligoAlimtalkClient.send(
                receiverPhone = receiverPhone,
                message = message,
                templateCode = templateCode,
            )
        }.onFailure { e ->
            log.error(
                "[PickupReminderAlimtalkEventListener] 픽업 리마인드 알림톡 발송 실패: participationId={}, triggerType={}",
                event.targetId, event.triggerType, e
            )
        }
    }

    companion object {
        private val PICKUP_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        private val PICKUP_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
