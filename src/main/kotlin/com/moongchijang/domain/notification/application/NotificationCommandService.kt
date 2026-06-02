package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.dto.NotificationUnreadCountResponse
import com.moongchijang.domain.notification.domain.entity.NotificationScope
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationCommandService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
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

        val scope = resolveNotificationScope(userId)
        val updatedCount = notificationRepository.markAllAsReadByUserIdAndScope(userId, scope)

        log.info(
            "[NotificationCommandService] 알림 전체 읽음 처리 완료: userId={}, scope={}, updatedCount={}",
            userId,
            scope,
            updatedCount
        )
        return updatedCount
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: Long): NotificationUnreadCountResponse {
        log.info("[NotificationCommandService] 미읽음 알림 개수 조회 시작: userId={}", userId)

        val scope = resolveNotificationScope(userId)
        val count = notificationRepository.countUnreadByUserIdAndScope(userId, scope)

        log.info(
            "[NotificationCommandService] 미읽음 알림 개수 조회 완료: userId={}, scope={}, count={}",
            userId,
            scope,
            count
        )
        return NotificationUnreadCountResponse(count = count)
    }

    private fun resolveNotificationScope(userId: Long): NotificationScope {
        val userRole = userRepository.findByIdAndDeletedAtIsNull(userId)?.role
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        return when (userRole) {
            UserRole.BUYER -> NotificationScope.BUYER
            UserRole.SELLER -> NotificationScope.OWNER
            UserRole.ADMIN -> NotificationScope.BUYER
        }
    }
}
