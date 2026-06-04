package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.dto.NotificationCategory
import com.moongchijang.domain.notification.application.dto.NotificationCursor
import com.moongchijang.domain.notification.domain.entity.NotificationScope
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.domain.entity.NotificationType
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.support.NotificationFixture
import com.moongchijang.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.time.ZoneId

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
            NotificationFixture.createNotification(
                user = user,
                id = 11L,
                occurredAt = occurredAt,
                triggerType = NotificationTriggerType.PICKUP_SAME_DAY_MORNING
            )
        )

        `when`(
            notificationRepository.findForListByScope(
                userId = 1L,
                scope = NotificationScope.BUYER,
                type = null,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(notifications)
        val result = service.getNotifications(
            userId = 1L,
            currentRole = UserRole.BUYER,
            category = NotificationCategory.ALL,
            cursor = null,
            limit = 20
        )

        verify(notificationRepository).findForListByScope(
            userId = 1L,
            scope = NotificationScope.BUYER,
            type = null,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 21)
        )
        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].id).isEqualTo(11L)
        assertThat(result.items[0].triggerType).isEqualTo(NotificationTriggerType.PICKUP_SAME_DAY_MORNING)
    }

    @Test
    fun `카테고리 APPLY로 조회할 때 APPLY type 필터 적용 검증`() {
        `when`(
            notificationRepository.findForListByScope(
                userId = 2L,
                scope = NotificationScope.BUYER,
                type = NotificationType.APPLY,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(emptyList())
        service.getNotifications(
            userId = 2L,
            currentRole = UserRole.BUYER,
            category = NotificationCategory.APPLY,
            cursor = null,
            limit = 20
        )

        verify(notificationRepository).findForListByScope(
            userId = 2L,
            scope = NotificationScope.BUYER,
            type = NotificationType.APPLY,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 21)
        )
    }

    @Test
    fun `카테고리 TODAY_PICKUP으로 조회할 때 사장님 당일 픽업 triggerType 필터 적용`() {
        `when`(
            notificationRepository.findForListByScopeAndTriggerTypes(
                userId = 21L,
                scope = NotificationScope.OWNER,
                triggerTypes = setOf(NotificationTriggerType.OWNER_PICKUP_SAME_DAY_MORNING),
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(emptyList())
        service.getNotifications(
            userId = 21L,
            currentRole = UserRole.SELLER,
            category = NotificationCategory.TODAY_PICKUP,
            cursor = null,
            limit = 20
        )

        verify(notificationRepository).findForListByScopeAndTriggerTypes(
            userId = 21L,
            scope = NotificationScope.OWNER,
            triggerTypes = setOf(NotificationTriggerType.OWNER_PICKUP_SAME_DAY_MORNING),
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 21)
        )
    }

    @Test
    fun `카테고리 REMINDER로 조회할 때 사장님 전날 픽업 triggerType 필터 적용`() {
        `when`(
            notificationRepository.findForListByScopeAndTriggerTypes(
                userId = 22L,
                scope = NotificationScope.OWNER,
                triggerTypes = setOf(NotificationTriggerType.OWNER_PICKUP_DAY_BEFORE_MORNING),
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(emptyList())
        service.getNotifications(
            userId = 22L,
            currentRole = UserRole.SELLER,
            category = NotificationCategory.REMINDER,
            cursor = null,
            limit = 20
        )

        verify(notificationRepository).findForListByScopeAndTriggerTypes(
            userId = 22L,
            scope = NotificationScope.OWNER,
            triggerTypes = setOf(NotificationTriggerType.OWNER_PICKUP_DAY_BEFORE_MORNING),
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 21)
        )
    }

    @Test
    fun `카테고리 CONFIRMED로 조회할 때 사장님 확정 triggerType 필터 적용`() {
        val confirmedTriggerTypes = setOf(
            NotificationTriggerType.OWNER_GROUPBUY_ACHIEVED_IMMEDIATE,
            NotificationTriggerType.OWNER_OPEN_REQUEST_APPROVED_IMMEDIATE,
            NotificationTriggerType.OWNER_ORDER_CONFIRM_REQUIRED_IMMEDIATE
        )
        `when`(
            notificationRepository.findForListByScopeAndTriggerTypes(
                userId = 23L,
                scope = NotificationScope.OWNER,
                triggerTypes = confirmedTriggerTypes,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(emptyList())
        service.getNotifications(
            userId = 23L,
            currentRole = UserRole.SELLER,
            category = NotificationCategory.CONFIRMED,
            cursor = null,
            limit = 20
        )

        verify(notificationRepository).findForListByScopeAndTriggerTypes(
            userId = 23L,
            scope = NotificationScope.OWNER,
            triggerTypes = confirmedTriggerTypes,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 21)
        )
    }

    @Test
    fun `카테고리 CANCELLED로 조회할 때 사장님 취소 triggerType 필터 적용`() {
        val cancelledTriggerTypes = setOf(
            NotificationTriggerType.OWNER_GROUPBUY_FAILED_IMMEDIATE,
            NotificationTriggerType.OWNER_ORDER_CANCELLED_IMMEDIATE,
            NotificationTriggerType.OWNER_OPEN_REQUEST_REJECTED_IMMEDIATE,
            NotificationTriggerType.OWNER_CLOSE_REQUEST_REJECTED_IMMEDIATE
        )
        `when`(
            notificationRepository.findForListByScopeAndTriggerTypes(
                userId = 24L,
                scope = NotificationScope.OWNER,
                triggerTypes = cancelledTriggerTypes,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(emptyList())
        service.getNotifications(
            userId = 24L,
            currentRole = UserRole.SELLER,
            category = NotificationCategory.CANCELLED,
            cursor = null,
            limit = 20
        )

        verify(notificationRepository).findForListByScopeAndTriggerTypes(
            userId = 24L,
            scope = NotificationScope.OWNER,
            triggerTypes = cancelledTriggerTypes,
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
            notificationRepository.findForListByScope(
                userId = 3L,
                scope = NotificationScope.BUYER,
                type = null,
                cursorOccurredAt = now,
                cursorId = 100L,
                pageable = PageRequest.of(0, 3)
            )
        ).thenReturn(listOf(n1, n2, n3))
        val result = service.getNotifications(
            userId = 3L,
            currentRole = UserRole.BUYER,
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
        val kstToday = LocalDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate()
        val today = NotificationFixture.createNotification(
            user = user,
            id = 41L,
            occurredAt = kstToday.atTime(10, 0)
        )
        val yesterday = NotificationFixture.createNotification(
            user = user,
            id = 40L,
            occurredAt = kstToday.minusDays(1).atTime(10, 0)
        )
        val older = NotificationFixture.createNotification(
            user = user,
            id = 39L,
            occurredAt = kstToday.minusDays(3).atTime(10, 0)
        )

        `when`(
            notificationRepository.findForListByScope(
                userId = 4L,
                scope = NotificationScope.BUYER,
                type = null,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(listOf(today, yesterday, older))
        val result = service.getNotifications(
            userId = 4L,
            currentRole = UserRole.BUYER,
            category = NotificationCategory.ALL,
            cursor = null,
            limit = 20
        )

        assertThat(result.items.map { it.section.name }).containsExactly("TODAY", "YESTERDAY", "OLDER")
    }

    @Test
    fun `UTC 자정 직전 알림도 KST 기준 오늘 섹션으로 분류한다`() {
        val user = UserFixture.createEmailUser(id = 6L)
        val kstToday = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val occurredAtUtc = kstToday.minusDays(1).atTime(23, 30)
        val notification = NotificationFixture.createNotification(
            user = user,
            id = 61L,
            occurredAt = occurredAtUtc
        )

        `when`(
            notificationRepository.findForListByScope(
                userId = 6L,
                scope = NotificationScope.BUYER,
                type = null,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21)
            )
        ).thenReturn(listOf(notification))

        val result = service.getNotifications(
            userId = 6L,
            currentRole = UserRole.BUYER,
            category = NotificationCategory.ALL,
            cursor = null,
            limit = 20
        )

        assertThat(result.items.single().section.name).isEqualTo("TODAY")
    }

    @Test
    fun `limit 범위를 벗어나 조회할 때 최소 최대 보정 적용`() {
        `when`(
            notificationRepository.findForListByScope(
                userId = 5L,
                scope = NotificationScope.BUYER,
                type = null,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 2)
            )
        ).thenReturn(emptyList())
        `when`(
            notificationRepository.findForListByScope(
                userId = 5L,
                scope = NotificationScope.BUYER,
                type = null,
                cursorOccurredAt = null,
                cursorId = null,
                pageable = PageRequest.of(0, 101)
            )
        ).thenReturn(emptyList())
        service.getNotifications(
            userId = 5L,
            currentRole = UserRole.BUYER,
            category = NotificationCategory.ALL,
            cursor = null,
            limit = 0
        )
        service.getNotifications(
            userId = 5L,
            currentRole = UserRole.BUYER,
            category = NotificationCategory.ALL,
            cursor = null,
            limit = 999
        )

        verify(notificationRepository).findForListByScope(
            userId = 5L,
            scope = NotificationScope.BUYER,
            type = null,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 2)
        )
        verify(notificationRepository).findForListByScope(
            userId = 5L,
            scope = NotificationScope.BUYER,
            type = null,
            cursorOccurredAt = null,
            cursorId = null,
            pageable = PageRequest.of(0, 101)
        )
    }
}
