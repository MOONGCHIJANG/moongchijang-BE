package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.notification.infrastructure.aligo.AligoAlimtalkClient
import com.moongchijang.domain.notification.infrastructure.aligo.AligoProperties
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class GroupBuyRequestStatusCommandService(
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val userRepository: UserRepository,
    private val aligoAlimtalkClient: AligoAlimtalkClient,
    private val aligoProperties: AligoProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun rejectRequest(
        requestId: Long,
        reason: String?,
        changedAt: LocalDateTime = LocalDateTime.now()
    ) {
        val request = groupBuyRequestRepository.findById(requestId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND) }

        request.markRejected(reason)
        groupBuyRequestStatusHistoryRepository.save(
            GroupBuyRequestStatusHistory(
                groupBuyRequestId = request.id,
                status = GroupBuyRequestStatus.REJECTED,
                changedAt = changedAt
            )
        )

        notificationEventPublisher.publishRequestRejected(
            requestId = request.id,
            requesterUserId = request.userId,
            occurredAt = changedAt
        )
        sendOpenFailAlimtalk(request.id)
        log.info(
            "[GroupBuyRequestStatusCommandService] 요청공구 거절 처리 및 알림 트리거 발행: requestId={}, requesterUserId={}",
            requestId, request.userId
        )
    }

    private fun sendOpenFailAlimtalk(requestId: Long) {
        runCatching {
            val request = groupBuyRequestRepository.findById(requestId)
                .orElseThrow { CustomException(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND) }
            val user = userRepository.findByIdAndDeletedAtIsNull(request.userId)
                ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
            val receiverPhone = user.phoneNumber?.trim().orEmpty()
            if (receiverPhone.isBlank()) {
                log.warn(
                    "[GroupBuyRequestStatusCommandService] 공구 개설 실패 알림톡 스킵(전화번호 없음): requestId={}, userId={}",
                    requestId,
                    request.userId,
                )
                return
            }

            val nickname = user.nickname ?: "고객"
            val pickupPlace = request.roadAddress ?: request.storeAddress ?: request.storeName
            val pickupDate = request.desiredPickupDate.format(PICKUP_DATE_FORMATTER)
            val message = """
                ${nickname}님, 공구 요청 심사 결과가 나왔어요.
                
                요청하신 공구를 개설하기 위해 뭉치장이 꼼꼼히 확인했어요.
                심사결과를 안내드려요.
                
                <요청하신 내역>
                - 상품명: ${request.productName}
                - 픽업 장소: ${pickupPlace}
                - 픽업 일시: ${pickupDate}
                
                
                ※ 뭉치장은 늘 행복한 디저트를 웨이팅없이 맛보실 수 있도록 최선을 다하겠습니다.
                
                - 팀 뭉치장 드림
            """.trimIndent()

            aligoAlimtalkClient.send(
                receiverPhone = receiverPhone,
                message = message,
                templateCode = aligoProperties.templateCodeGroupBuyOpenFailed,
            )
        }.onFailure { e ->
            log.error(
                "[GroupBuyRequestStatusCommandService] 공구 개설 실패 알림톡 발송 실패: requestId={}",
                requestId,
                e,
            )
        }
    }

    companion object {
        private val PICKUP_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}
