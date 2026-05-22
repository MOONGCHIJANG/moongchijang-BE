package com.moongchijang.domain.participation.application

import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ParticipationPickupCommandService(
    private val participationRepository: ParticipationRepository,
    private val userRepository: UserRepository,
    private val notificationEventPublisher: NotificationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun completePickup(participationId: Long, processedByUserId: Long, pickedUpAt: LocalDateTime = LocalDateTime.now()) {
        val participation = participationRepository.findByIdForUpdate(participationId)
            .orElseThrow { CustomException(ErrorCode.PARTICIPATION_NOT_FOUND) }
        val processor = userRepository.findByIdAndDeletedAtIsNull(processedByUserId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        participation.markPickedUp(processedBy = processor, pickedUpAt = pickedUpAt)
        val userId = participation.user.id ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        notificationEventPublisher.publishPickupCompleted(
            groupBuyId = participation.groupBuy.id,
            userId = userId,
            participationId = participation.id,
            occurredAt = pickedUpAt
        )
        log.info(
            "[ParticipationPickupCommandService] 픽업 완료 처리 및 알림 트리거 발행: participationId={}, userId={}, processedByUserId={}",
            participationId, userId, processedByUserId
        )
    }
}
