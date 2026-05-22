package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyStatusTransitionScheduler
import com.moongchijang.domain.groupbuy.application.GroupBuyStatusTransitionService
import com.moongchijang.domain.groupbuy.infrastructure.lock.RedisLockUtil
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GroupBuyStatusTransitionSchedulerTest {

    @Mock
    private lateinit var groupBuyStatusTransitionService: GroupBuyStatusTransitionService

    @Mock
    private lateinit var redisLockUtil: RedisLockUtil

    @Test
    fun `스케줄러 실행 시 상태 전이 서비스 호출`() {
        val scheduler = GroupBuyStatusTransitionScheduler(
            groupBuyStatusTransitionService = groupBuyStatusTransitionService,
            redisLockUtil = redisLockUtil,
            lockWaitMs = 100L,
            lockLeaseMs = 55_000L
        )
        `when`(redisLockUtil.tryLockOrThrow("groupBuy:status-transition", 100L, 55_000L)).thenReturn("token")
        `when`(redisLockUtil.unlock("groupBuy:status-transition", "token")).thenReturn(true)

        scheduler.transitionExpiredGroupBuys()

        verify(groupBuyStatusTransitionService).transitionExpiredGroupBuys()
        verify(redisLockUtil).unlock("groupBuy:status-transition", "token")
    }

    @Test
    fun `분산락 획득 실패 시 스케줄러 실행 스킵`() {
        val scheduler = GroupBuyStatusTransitionScheduler(
            groupBuyStatusTransitionService = groupBuyStatusTransitionService,
            redisLockUtil = redisLockUtil,
            lockWaitMs = 100L,
            lockLeaseMs = 55_000L
        )
        `when`(redisLockUtil.tryLockOrThrow("groupBuy:status-transition", 100L, 55_000L))
            .thenThrow(CustomException(ErrorCode.GROUPBUY_LOCK_ACQUISITION_FAILED))

        assertDoesNotThrow {
            scheduler.transitionExpiredGroupBuys()
        }

        verify(groupBuyStatusTransitionService, never()).transitionExpiredGroupBuys()
    }
}
