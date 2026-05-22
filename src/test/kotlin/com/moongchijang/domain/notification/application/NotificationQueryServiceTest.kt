package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.dto.NotificationCategory
import com.moongchijang.domain.notification.application.dto.NotificationCursor
import com.moongchijang.domain.notification.domain.entity.NotificationType
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.support.NotificationFixture
import com.moongchijang.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class NotificationQueryServiceTest {

    @Mock
    private lateinit var notificationRepository: NotificationRepository

    private val service by lazy { NotificationQueryService(notificationRepository) }

    @Test
    fun `카테고리 ALL로 조회할 때 type 필터 없는 목록 반환`() {
        val user = UserFixture.createEmailUser(id = 1L)
        val occurredAt = LocalDateTime.now().minusHours(1).withNano(0)
        val notifications = listOf(
            NotificationFixture.createNotification(user = user, id = 11L, occurredAt = occurredAt)
        )

        `when`(
            notificationRepository.findForList(
                userId = 1L,
                type = null,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(notifications)

        val result = service.getNotifications(
            userId = 1L,
            category = NotificationCategory.ALL,
            cursor = null,
            limit = 20
        )

        verify(notificationRepository).findForList(
            userId = 1L,
            type = null,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 21)
        )
        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].id).isEqualTo(11L)
    }

    @Test
    fun `카테고리 APPLY로 조회할 때 APPLY type 필터 적용 검증`() {
        `when`(
            notificationRepository.findForList(
                userId = 2L,
                type = NotificationType.APPLY,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(emptyList())

        service.getNotifications(
            userId = 2L,
            category = NotificationCategory.APPLY,
            cursor = null,
            limit = 20
        )

        verify(notificationRepository).findForList(
            userId = 2L,
            type = NotificationType.APPLY,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 21)
        )
    }

    @Test
    fun `커서 기반으로 조회할 때 hasNext와 nextCursor 반환`() {
        val user = UserFixture.createEmailUser(id = 3L)
        val now = LocalDateTime.now().withNano(0)
        val n1 = NotificationFixture.createNotification(user = user, id = 31L, occurredAt = now.minusMinutes(1))
        val n2 = NotificationFixture.createNotification(user = user, id = 30L, occurredAt = now.minusMinutes(2))
        val n3 = NotificationFixture.createNotification(user = user, id = 29L, occurredAt = now.minusMinutes(3))
        val cursor = NotificationCursor(now, 100L).encode()

        `when`(
            notificationRepository.findForList(
                userId = 3L,
                type = null,
                cursorOccurredAt = now,
                cursorId = 100L,
                pageable = PageRequest.of(0, 3)
            )
        ).thenReturn(listOf(n1, n2, n3))

        val result = service.getNotifications(
            userId = 3L,
            category = NotificationCategory.ALL,
            cursor = cursor,
            limit = 2
        )

        assertThat(result.hasNext).isTrue()
        assertThat(result.items).hasSize(2)
        assertThat(result.nextCursor).isEqualTo(NotificationCursor(n2.occurredAt, n2.id).encode())
    }

    @Test
    fun `알림 목록을 조회할 때 TODAY YESTERDAY OLDER 섹션 분류 반환`() {
        val user = UserFixture.createEmailUser(id = 4L)
        val now = LocalDateTime.now().withNano(0)
        val today = NotificationFixture.createNotification(user = user, id = 41L, occurredAt = now)
        val yesterday = NotificationFixture.createNotification(user = user, id = 40L, occurredAt = now.minusDays(1))
        val older = NotificationFixture.createNotification(user = user, id = 39L, occurredAt = now.minusDays(3))

        `when`(
            notificationRepository.findForList(
                userId = 4L,
                type = null,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(listOf(today, yesterday, older))

        val result = service.getNotifications(
            userId = 4L,
            category = NotificationCategory.ALL,
            cursor = null,
            limit = 20
        )

        assertThat(result.items.map { it.section.name }).containsExactly("TODAY", "YESTERDAY", "OLDER")
    }

    @Test
    fun `limit 범위를 벗어나 조회할 때 최소 최대 보정 적용`() {
        `when`(
            notificationRepository.findForList(
                userId = 5L,
                type = null,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 2)
            )
        ).thenReturn(emptyList())
        `when`(
            notificationRepository.findForList(
                userId = 5L,
                type = null,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 101)
            )
        ).thenReturn(emptyList())

        service.getNotifications(
            userId = 5L,
            category = NotificationCategory.ALL,
            cursor = null,
            limit = 0
        )
        service.getNotifications(
            userId = 5L,
            category = NotificationCategory.ALL,
            cursor = null,
            limit = 999
        )

        verify(notificationRepository).findForList(
            userId = 5L,
            type = null,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 2)
        )
        verify(notificationRepository).findForList(
            userId = 5L,
            type = null,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 101)
        )
    }
}
