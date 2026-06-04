package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.notification.application.event.NotificationImmediateTriggerEvent
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.infrastructure.aligo.AligoAlimtalkClient
import com.moongchijang.domain.notification.infrastructure.aligo.AligoMessageFormatter
import com.moongchijang.domain.notification.infrastructure.aligo.AligoProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.security.crypto.PersonalInfoManager
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.format.DateTimeFormatter

@Component
class GroupBuyRequestRejectedAlimtalkEventListener(
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val aligoAlimtalkClient: AligoAlimtalkClient,
    private val aligoProperties: AligoProperties,
    private val personalInfoManager: PersonalInfoManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("notificationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: NotificationImmediateTriggerEvent) {
        if (event.triggerType != NotificationTriggerType.REQUEST_REJECTED_IMMEDIATE) {
            return
        }

        val requestId = event.targetId
        runCatching {
            val request = groupBuyRequestRepository.findById(requestId)
                .orElseThrow { CustomException(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND) }
            val user = request.user
            val userId = requireNotNull(user.id) { "GroupBuyRequest.user.id must not be null" }
            if (user.deletedAt != null) {
                log.warn(
                    "[GroupBuyRequestRejectedAlimtalkEventListener] 공구 개설 실패 알림톡 스킵(사용자 탈퇴): requestId={}, userId={}",
                    requestId,
                    userId,
                )
                return
            }
            val receiverPhone = personalInfoManager.decryptIfNeeded(user.phoneNumber)?.trim().orEmpty()
            if (receiverPhone.isBlank()) {
                log.warn(
                    "[GroupBuyRequestRejectedAlimtalkEventListener] 공구 개설 실패 알림톡 스킵(전화번호 없음): requestId={}, userId={}",
                    requestId,
                    userId,
                )
                return
            }

            val message = AligoMessageFormatter.groupBuyOpenFailed(
                nickname = user.nickname ?: "고객",
                productName = request.productName,
                pickupPlace = request.roadAddress ?: request.storeAddress ?: request.storeName,
                pickupDate = request.desiredPickupDate.format(PICKUP_DATE_FORMATTER),
            )

            runCatching {
                aligoAlimtalkClient.send(
                    receiverPhone = receiverPhone,
                    message = message,
                    templateCode = aligoProperties.templateCodeGroupBuyOpenFailed,
                )
            }.onFailure { e ->
                log.error(
                    "[GroupBuyRequestRejectedAlimtalkEventListener] 공구 개설 실패 알림톡 발송 실패: requestId={}",
                    requestId,
                    e,
                )
            }
        }.onFailure { e ->
            log.error(
                "[GroupBuyRequestRejectedAlimtalkEventListener] 공구 개설 실패 알림톡 처리 실패: requestId={}",
                requestId,
                e,
            )
        }
    }

    companion object {
        private val PICKUP_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}
