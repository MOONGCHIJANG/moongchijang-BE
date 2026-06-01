package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyStatusTransitionService
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.notification.application.discord.AdminDiscordAlertService
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class GroupBuyStatusTransitionServiceTest {

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var transactionManager: PlatformTransactionManager

    @Mock
    private lateinit var transactionStatus: TransactionStatus

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var notificationEventPublisher: NotificationEventPublisher

    @Mock
    private lateinit var adminDiscordAlertService: AdminDiscordAlertService

    private lateinit var service: GroupBuyStatusTransitionService

    @BeforeEach
    fun setUp() {
        `when`(transactionManager.getTransaction(any())).thenReturn(transactionStatus)
        service = GroupBuyStatusTransitionService(
            groupBuyRepository,
            participationRepository,
            notificationEventPublisher,
            adminDiscordAlertService,
            transactionManager,
            500
        )
    }

    @Test
    fun `deadline 경과 대상 조회 시 상태 자동 전이`() {
        val now = LocalDateTime.now()
        val inProgress = GroupBuyFixture.createGroupBuy(
            id = 1L,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = now.minusMinutes(1)
        )
        val achieved = GroupBuyFixture.createGroupBuy(
            id = 2L,
            status = GroupBuyStatus.ACHIEVED,
            deadline = now.minusMinutes(1)
        )
        val pageable = PageRequest.of(0, 500, Sort.by(Sort.Order.asc("deadline"), Sort.Order.asc("id")))

        `when`(
            groupBuyRepository.findByStatusInAndDeadlineLessThanEqual(
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                now,
                pageable
            )
        ).thenReturn(listOf(inProgress, achieved))
        service.transitionExpiredGroupBuysAt(now)

        assertEquals(GroupBuyStatus.FAILED, inProgress.status)
        assertEquals(GroupBuyStatus.COMPLETED, achieved.status)
        verify(participationRepository).updateStatusByGroupBuyIdAndStatus(
            groupBuyId = 1L,
            oldStatus = ParticipationStatus.PAID_WAITING_GOAL,
            newStatus = ParticipationStatus.REFUND_PENDING,
            cancelledAt = now
        )
    }

    @Test
    fun `자동 전이 실행 시 IN_PROGRESS 및 ACHIEVED 상태 조회`() {
        val now = LocalDateTime.now()
        val pageable = PageRequest.of(0, 500, Sort.by(Sort.Order.asc("deadline"), Sort.Order.asc("id")))
        `when`(
            groupBuyRepository.findByStatusInAndDeadlineLessThanEqual(
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                now,
                pageable
            )
        ).thenReturn(emptyList())

        service.transitionExpiredGroupBuysAt(now)

        verify(groupBuyRepository).findByStatusInAndDeadlineLessThanEqual(
            listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
            now,
            pageable
        )
    }

    @Test
    fun `자동 전이 대상에 종료 상태 포함 시 상태 유지`() {
        val now = LocalDateTime.now()
        val completed = GroupBuyFixture.createGroupBuy(
            id = 3L,
            status = GroupBuyStatus.COMPLETED,
            deadline = now.minusMinutes(1)
        )
        val failed = GroupBuyFixture.createGroupBuy(
            id = 4L,
            status = GroupBuyStatus.FAILED,
            deadline = now.minusMinutes(1)
        )
        val closed = GroupBuyFixture.createGroupBuy(
            id = 5L,
            status = GroupBuyStatus.CLOSED,
            deadline = now.minusMinutes(1)
        )
        val pageable = PageRequest.of(0, 500, Sort.by(Sort.Order.asc("deadline"), Sort.Order.asc("id")))

        `when`(
            groupBuyRepository.findByStatusInAndDeadlineLessThanEqual(
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                now,
                pageable
            )
        ).thenReturn(listOf(completed, failed, closed))

        service.transitionExpiredGroupBuysAt(now)

        assertEquals(GroupBuyStatus.COMPLETED, completed.status)
        assertEquals(GroupBuyStatus.FAILED, failed.status)
        assertEquals(GroupBuyStatus.CLOSED, closed.status)
    }

    @Test
    fun `자동 전이 실행 시 batch size 기준 반복 처리`() {
        val now = LocalDateTime.now()
        val batchService = GroupBuyStatusTransitionService(
            groupBuyRepository,
            participationRepository,
            notificationEventPublisher,
            adminDiscordAlertService,
            transactionManager,
            2
        )
        val pageable = PageRequest.of(0, 2, Sort.by(Sort.Order.asc("deadline"), Sort.Order.asc("id")))
        val first = GroupBuyFixture.createGroupBuy(id = 11L, status = GroupBuyStatus.IN_PROGRESS, deadline = now.minusMinutes(1))
        val second = GroupBuyFixture.createGroupBuy(id = 12L, status = GroupBuyStatus.ACHIEVED, deadline = now.minusMinutes(1))
        val third = GroupBuyFixture.createGroupBuy(id = 13L, status = GroupBuyStatus.IN_PROGRESS, deadline = now.minusMinutes(1))

        `when`(
            groupBuyRepository.findByStatusInAndDeadlineLessThanEqual(
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                now,
                pageable
            )
        ).thenReturn(listOf(first, second), listOf(third), emptyList())
        batchService.transitionExpiredGroupBuysAt(now)

        assertEquals(GroupBuyStatus.FAILED, first.status)
        assertEquals(GroupBuyStatus.COMPLETED, second.status)
        assertEquals(GroupBuyStatus.FAILED, third.status)
        verify(participationRepository).updateStatusByGroupBuyIdAndStatus(
            groupBuyId = 11L,
            oldStatus = ParticipationStatus.PAID_WAITING_GOAL,
            newStatus = ParticipationStatus.REFUND_PENDING,
            cancelledAt = now
        )
        verify(participationRepository).updateStatusByGroupBuyIdAndStatus(
            groupBuyId = 13L,
            oldStatus = ParticipationStatus.PAID_WAITING_GOAL,
            newStatus = ParticipationStatus.REFUND_PENDING,
            cancelledAt = now
        )
    }

    @Test
    fun `IN_PROGRESS 공구가 실패하면 참여를 환불대기로 전환한다`() {
        val now = LocalDateTime.now()
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 21L,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = now.minusMinutes(1)
        )
        val pageable = PageRequest.of(0, 500, Sort.by(Sort.Order.asc("deadline"), Sort.Order.asc("id")))
        `when`(
            groupBuyRepository.findByStatusInAndDeadlineLessThanEqual(
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                now,
                pageable
            )
        ).thenReturn(listOf(groupBuy))
        service.transitionExpiredGroupBuysAt(now)

        assertEquals(GroupBuyStatus.FAILED, groupBuy.status)
        verify(participationRepository).updateStatusByGroupBuyIdAndStatus(
            groupBuyId = 21L,
            oldStatus = ParticipationStatus.PAID_WAITING_GOAL,
            newStatus = ParticipationStatus.REFUND_PENDING,
            cancelledAt = now
        )
    }
}
