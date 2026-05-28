package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.dto.NotificationUnreadCountResponse
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationCommandService(
    private val notificationRepository: NotificationRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun markAsRead(userId: Long, notificationId: Long) {
        log.info(
            "[NotificationCommandService] 알림 단건 읽음 처리 시작: userId={}, notificationId={}",
            userId,
            notificationId
        )

        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { CustomException(ErrorCode.NOTIFICATION_NOT_FOUND) }

        if (notification.user.id != userId) {
            throw CustomException(ErrorCode.NOTIFICATION_FORBIDDEN)
        }
        notification.markAsRead()

        log.info(
            "[NotificationCommandService] 알림 단건 읽음 처리 완료: userId={}, notificationId={}, isRead={}",
            userId,
            notificationId,
            notification.isRead
        )
    }

    @Transactional
    fun markAllAsRead(userId: Long): Int {
        log.info("[NotificationCommandService] 알림 전체 읽음 처리 시작: userId={}", userId)

        val updatedCount = notificationRepository.markAllAsReadByUserId(userId)

        log.info(
            "[NotificationCommandService] 알림 전체 읽음 처리 완료: userId={}, updatedCount={}",
            userId,
            updatedCount
        )
        return updatedCount
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: Long): NotificationUnreadCountResponse {
        log.info("[NotificationCommandService] 미읽음 알림 개수 조회 시작: userId={}", userId)

        val count = notificationRepository.countUnreadByUserId(userId)

        log.info(
            "[NotificationCommandService] 미읽음 알림 개수 조회 완료: userId={}, count={}",
            userId,
            count
        )
        return NotificationUnreadCountResponse(count = count)
    }
}
