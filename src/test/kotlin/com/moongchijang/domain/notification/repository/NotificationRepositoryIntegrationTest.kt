package com.moongchijang.domain.notification.repository

import com.moongchijang.domain.notification.domain.entity.Notification
import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType
import com.moongchijang.domain.notification.domain.entity.NotificationType
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationRepositoryIntegrationTest {

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var em: EntityManager

    @Test
    fun `최신순 정렬 occurredAt desc id desc`() {
        val user = persistUser("repo-sort@test.com")
        val sameTime = LocalDateTime.of(2026, 5, 22, 9, 0, 0)

        val newer = persistNotification(user = user, type = NotificationType.PICKUP, occurredAt = sameTime.plusMinutes(1))
        val tieLowerId = persistNotification(user = user, type = NotificationType.PICKUP, occurredAt = sameTime)
        val tieHigherId = persistNotification(user = user, type = NotificationType.PICKUP, occurredAt = sameTime)

        flushAndClear()

        val result = notificationRepository.findForList(
            userId = user.id!!,
            type = null,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(result.map { it.id }).containsExactly(newer.id, tieHigherId.id, tieLowerId.id)
    }

    @Test
    fun `카테고리 type 필터 적용`() {
        val user = persistUser("repo-filter@test.com")
        persistNotification(user = user, type = NotificationType.APPLY, occurredAt = LocalDateTime.now().minusMinutes(1))
        persistNotification(user = user, type = NotificationType.WISH, occurredAt = LocalDateTime.now().minusMinutes(2))

        flushAndClear()

        val result = notificationRepository.findForList(
            userId = user.id!!,
            type = NotificationType.APPLY,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(NotificationType.APPLY)
    }

    @Test
    fun `커서 이후 데이터 조회`() {
        val user = persistUser("repo-cursor@test.com")
        val base = LocalDateTime.of(2026, 5, 22, 9, 0, 0)

        persistNotification(user = user, type = NotificationType.PICKUP, occurredAt = base.plusMinutes(1))
        val sameTimeLowId = persistNotification(user = user, type = NotificationType.PICKUP, occurredAt = base)
        val sameTimeHighId = persistNotification(user = user, type = NotificationType.PICKUP, occurredAt = base)
        val older = persistNotification(user = user, type = NotificationType.PICKUP, occurredAt = base.minusMinutes(1))

        flushAndClear()

        val result = notificationRepository.findForList(
            userId = user.id!!,
            type = null,
            cursorOccurredAt = base,
            cursorId = sameTimeHighId.id,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(result.map { it.id }).containsExactly(sameTimeLowId.id, older.id)
    }

    private fun persistUser(email: String): User {
        val user = User(
            provider = AuthProvider.EMAIL,
            email = email,
            passwordHash = "pw",
            nickname = "tester",
            role = UserRole.BUYER,
            signupCompleted = true
        )
        em.persist(user)
        return user
    }

    private fun persistNotification(
        user: User,
        type: NotificationType,
        occurredAt: LocalDateTime
    ): Notification {
        val notification = Notification(
            user = user,
            type = type,
            title = "알림",
            body = "본문",
            isRead = false,
            occurredAt = occurredAt,
            targetId = 100L,
            deeplinkType = NotificationDeeplinkType.PICKUP_GUIDE
        )
        em.persist(notification)
        return notification
    }

    private fun flushAndClear() {
        em.flush()
        em.clear()
    }
}
