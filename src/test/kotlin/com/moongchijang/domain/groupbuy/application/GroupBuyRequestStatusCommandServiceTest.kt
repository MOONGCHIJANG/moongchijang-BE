package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class GroupBuyRequestStatusCommandServiceTest {

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    @Mock
    private lateinit var groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository

    @Mock
    private lateinit var notificationEventPublisher: NotificationEventPublisher

    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-23T03:00:00Z"), ZoneOffset.UTC)

    private val service by lazy {
        GroupBuyRequestStatusCommandService(
            groupBuyRequestRepository = groupBuyRequestRepository,
            groupBuyRequestStatusHistoryRepository = groupBuyRequestStatusHistoryRepository,
            notificationEventPublisher = notificationEventPublisher,
            clock = clock,
        )
    }

    @Test
    fun `요청공구를 거절할 때 거절 상태 반영과 즉시 알림 이벤트 발행`() {
        val now = LocalDateTime.of(2026, 5, 23, 15, 0)
        val request = GroupBuyFixture.createGroupBuyRequest(userId = 7L).apply { id = 301L }

        `when`(groupBuyRequestRepository.findById(301L)).thenReturn(Optional.of(request))
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenAnswer { it.arguments[0] }

        service.rejectRequest(requestId = 301L, reason = "품절", changedAt = now)

        assertEquals(GroupBuyRequestStatus.REJECTED, request.status)
        assertEquals("품절", request.rejectionReason)
        verify(notificationEventPublisher).publishRequestRejected(
            requestId = 301L,
            requesterUserId = 7L,
            occurredAt = now
        )
    }
}
