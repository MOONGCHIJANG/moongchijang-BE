package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.NotificationFixture
import com.moongchijang.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class NotificationCommandServiceTest {

    @Mock
    private lateinit var notificationRepository: NotificationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private val service by lazy { NotificationCommandService(notificationRepository, userRepository) }

    @Test
    fun `같은 알림을 반복 읽음 처리할 때 멱등 동작 보장`() {
        val user = UserFixture.createEmailUser(id = 1L)
        val unread = NotificationFixture.createNotification(
            user = user,
            id = 101L,
            isRead = false,
            occurredAt = LocalDateTime.now().minusMinutes(10)
        )
        val alreadyRead = NotificationFixture.createNotification(
            user = user,
            id = 102L,
            isRead = true,
            occurredAt = LocalDateTime.now().minusMinutes(5)
        )

        `when`(notificationRepository.findById(101L)).thenReturn(Optional.of(unread))
        `when`(notificationRepository.findById(102L)).thenReturn(Optional.of(alreadyRead))

        assertDoesNotThrow { service.markAsRead(userId = 1L, notificationId = 101L) }
        assertDoesNotThrow { service.markAsRead(userId = 1L, notificationId = 102L) }

        assertThat(unread.isRead).isTrue()
        assertThat(alreadyRead.isRead).isTrue()
    }

    @Test
    fun `다른 사용자 알림을 읽음 처리할 때 권한 예외 반환`() {
        val owner = UserFixture.createEmailUser(id = 2L)
        val notification = NotificationFixture.createNotification(
            user = owner,
            id = 201L,
            occurredAt = LocalDateTime.now()
        )
        `when`(notificationRepository.findById(201L)).thenReturn(Optional.of(notification))

        val exception = assertThrows(CustomException::class.java) {
            service.markAsRead(userId = 999L, notificationId = 201L)
        }

        assertEquals(ErrorCode.NOTIFICATION_FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 알림을 읽음 처리할 때 대상 없음 예외 반환`() {
        `when`(notificationRepository.findById(301L)).thenReturn(Optional.empty())

        val exception = assertThrows(CustomException::class.java) {
            service.markAsRead(userId = 3L, notificationId = 301L)
        }

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `전체 읽음 처리를 반복 호출할 때 멱등 동작 보장`() {
        `when`(userRepository.findByIdAndDeletedAtIsNull(4L)).thenReturn(UserFixture.createEmailUser(id = 4L))
        `when`(notificationRepository.markAllAsReadByUserIdAndScope(4L, com.moongchijang.domain.notification.domain.entity.NotificationScope.BUYER)).thenReturn(5, 0)

        val firstUpdated = service.markAllAsRead(4L)
        val secondUpdated = service.markAllAsRead(4L)

        assertEquals(5, firstUpdated)
        assertEquals(0, secondUpdated)
    }

    @Test
    fun `미읽음 개수를 조회할 때 count 반환`() {
        `when`(userRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(
            UserFixture.createEmailUser(id = 5L).apply { role = UserRole.SELLER }
        )
        `when`(
            notificationRepository.countUnreadByUserIdAndScope(
                5L,
                com.moongchijang.domain.notification.domain.entity.NotificationScope.OWNER
            )
        ).thenReturn(7L)

        val response = service.getUnreadCount(5L)

        assertEquals(7L, response.count)
        verify(notificationRepository, never()).markAllAsReadByUserIdAndScope(5L, com.moongchijang.domain.notification.domain.entity.NotificationScope.OWNER)
    }
}
