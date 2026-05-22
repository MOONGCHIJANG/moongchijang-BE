package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyStatusTransitionScheduler
import com.moongchijang.domain.groupbuy.application.GroupBuyStatusTransitionService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GroupBuyStatusTransitionSchedulerTest {

    @Mock
    private lateinit var groupBuyStatusTransitionService: GroupBuyStatusTransitionService

    @InjectMocks
    private lateinit var scheduler: GroupBuyStatusTransitionScheduler

    @Test
    fun `스케줄러 실행 시 상태 전이 서비스 호출`() {
        scheduler.transitionExpiredGroupBuys()

        verify(groupBuyStatusTransitionService).transitionExpiredGroupBuys()
    }
}
