package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyStatusTransitionService
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class GroupBuyStatusTransitionServiceTest {

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @InjectMocks
    private lateinit var service: GroupBuyStatusTransitionService

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

        `when`(
            groupBuyRepository.findAllByStatusInAndDeadlineLessThanEqual(
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                now
            )
        ).thenReturn(listOf(inProgress, achieved))

        service.transitionExpiredGroupBuysAt(now)

        assertEquals(GroupBuyStatus.FAILED, inProgress.status)
        assertEquals(GroupBuyStatus.COMPLETED, achieved.status)
    }

    @Test
    fun `자동 전이 실행 시 IN_PROGRESS 및 ACHIEVED 상태 조회`() {
        val now = LocalDateTime.now()
        `when`(
            groupBuyRepository.findAllByStatusInAndDeadlineLessThanEqual(
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                now
            )
        ).thenReturn(emptyList())

        service.transitionExpiredGroupBuysAt(now)

        verify(groupBuyRepository).findAllByStatusInAndDeadlineLessThanEqual(
            listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
            now
        )
    }
}
