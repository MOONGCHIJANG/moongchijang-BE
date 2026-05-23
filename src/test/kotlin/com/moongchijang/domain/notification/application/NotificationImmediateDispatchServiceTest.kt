package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.event.NotificationImmediateTriggerEvent
import com.moongchijang.domain.notification.application.template.NotificationTemplateRegistry
import com.moongchijang.domain.notification.application.template.NotificationTemplateRenderer
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.notification.domain.entity.NotificationDispatchHistory
import com.moongchijang.domain.notification.domain.entity.NotificationDispatchStatus
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.domain.repository.NotificationDispatchHistoryRepository
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class NotificationImmediateDispatchServiceTest {

    @Mock
    private lateinit var notificationRepository: NotificationRepository

    @Mock
    private lateinit var notificationDispatchHistoryRepository: NotificationDispatchHistoryRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    private val notificationTemplateRegistry = NotificationTemplateRegistry()
    private val notificationTemplateRenderer = NotificationTemplateRenderer(notificationTemplateRegistry)

    private val service by lazy {
        NotificationImmediateDispatchService(
            notificationRepository = notificationRepository,
            notificationDispatchHistoryRepository = notificationDispatchHistoryRepository,
            userRepository = userRepository,
            groupBuyRepository = groupBuyRepository,
            groupBuyRequestRepository = groupBuyRequestRepository,
            notificationTemplateRegistry = notificationTemplateRegistry,
            notificationTemplateRenderer = notificationTemplateRenderer
        )
    }

    @Test
    fun `같은 중복키 이력이 존재할 때 즉시 발송 스킵`() {
        val event = NotificationImmediateTriggerEvent(
            triggerType = NotificationTriggerType.APPLY_PAYMENT_SUCCESS_IMMEDIATE,
            targetId = 101L,
            userIds = listOf(1L),
            scheduleKey = "payment-success:o-1",
            occurredAt = LocalDateTime.now()
        )
        `when`(
            notificationDispatchHistoryRepository.findByUserIdAndTriggerTypeAndTargetIdAndScheduleKey(
                userId = 1L,
                triggerType = NotificationTriggerType.APPLY_PAYMENT_SUCCESS_IMMEDIATE,
                targetId = 101L,
                scheduleKey = "payment-success:o-1"
            )
        ).thenReturn(Optional.of(mockHistory(userId = 1L, scheduleKey = "payment-success:o-1")))

        service.dispatch(event)

        verify(notificationRepository, never()).save(any())
    }

    @Test
    fun `사용자가 존재하지 않을 때 실패 이력 상태 저장`() {
        val event = NotificationImmediateTriggerEvent(
            triggerType = NotificationTriggerType.APPLY_PAYMENT_SUCCESS_IMMEDIATE,
            targetId = 102L,
            userIds = listOf(2L),
            scheduleKey = "payment-success:o-2",
            occurredAt = LocalDateTime.now()
        )
        val pending = mockHistory(userId = 2L, scheduleKey = "payment-success:o-2")

        `when`(
            notificationDispatchHistoryRepository.findByUserIdAndTriggerTypeAndTargetIdAndScheduleKey(
                userId = 2L,
                triggerType = NotificationTriggerType.APPLY_PAYMENT_SUCCESS_IMMEDIATE,
                targetId = 102L,
                scheduleKey = "payment-success:o-2"
            )
        ).thenReturn(Optional.empty())
        `when`(notificationDispatchHistoryRepository.save(any(NotificationDispatchHistory::class.java))).thenReturn(pending)
        `when`(userRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(null)

        service.dispatch(event)

        assertEquals(NotificationDispatchStatus.FAILED, pending.status)
        verify(notificationRepository, never()).save(any())
    }

    @Test
    fun `사용자와 이력이 유효할 때 알림 저장`() {
        val now = LocalDateTime.now()
        val event = NotificationImmediateTriggerEvent(
            triggerType = NotificationTriggerType.WISH_TARGET_ACHIEVED_IMMEDIATE,
            targetId = 103L,
            userIds = listOf(3L),
            scheduleKey = "wish-target-achieved:103",
            occurredAt = now
        )
        val pending = mockHistory(userId = 3L, scheduleKey = "wish-target-achieved:103")
        val user = UserFixture.createEmailUser(id = 3L)

        `when`(
            notificationDispatchHistoryRepository.findByUserIdAndTriggerTypeAndTargetIdAndScheduleKey(
                userId = 3L,
                triggerType = NotificationTriggerType.WISH_TARGET_ACHIEVED_IMMEDIATE,
                targetId = 103L,
                scheduleKey = "wish-target-achieved:103"
            )
        ).thenReturn(Optional.empty())
        `when`(notificationDispatchHistoryRepository.save(any(NotificationDispatchHistory::class.java))).thenReturn(pending)
        `when`(userRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(user)
        `when`(notificationRepository.save(any())).thenAnswer { it.arguments[0] }

        service.dispatch(event)

        assertEquals(NotificationDispatchStatus.SUCCESS, pending.status)
        verify(notificationRepository).save(any())
    }

    @Test
    fun `실패 이력 재시도 시 대상 건 재발송 처리`() {
        val now = LocalDateTime.of(2026, 5, 23, 9, 0)
        val failedHistory = NotificationDispatchHistory(
            userId = 9L,
            triggerType = NotificationTriggerType.APPLY_GROUPBUY_FAILED_IMMEDIATE,
            targetId = 901L,
            scheduleKey = "groupbuy-failed:901",
            status = NotificationDispatchStatus.FAILED,
            retryCount = 1,
            nextRetryAt = now.minusMinutes(1)
        )
        val user = UserFixture.createEmailUser(id = 9L)

        `when`(
            notificationDispatchHistoryRepository.findByStatusInAndNextRetryAtLessThanEqual(
                statuses = listOf(NotificationDispatchStatus.FAILED),
                nextRetryAt = now
            )
        ).thenReturn(listOf(failedHistory))
        `when`(userRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(user)
        `when`(notificationRepository.save(any())).thenAnswer { it.arguments[0] }

        service.retryFailedDispatches(now)

        assertEquals(NotificationDispatchStatus.SUCCESS, failedHistory.status)
        verify(notificationRepository).save(any())
    }

    private fun mockHistory(userId: Long, scheduleKey: String): NotificationDispatchHistory {
        return NotificationDispatchHistory(
            userId = userId,
            triggerType = NotificationTriggerType.APPLY_PAYMENT_SUCCESS_IMMEDIATE,
            targetId = 1L,
            scheduleKey = scheduleKey
        )
    }
}
