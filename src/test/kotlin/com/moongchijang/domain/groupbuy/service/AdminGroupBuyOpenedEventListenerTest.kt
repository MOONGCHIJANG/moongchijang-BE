package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.AdminGroupBuyOpenedEventListener
import com.moongchijang.domain.groupbuy.application.AdminGroupBuyRequestActionService
import com.moongchijang.domain.groupbuy.application.GroupBuyOpenRequestService
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminGroupBuyOpenedEventListenerTest {

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var groupBuyOpenRequestService: GroupBuyOpenRequestService

    @InjectMocks
    private lateinit var listener: AdminGroupBuyOpenedEventListener

    @Test
    fun `공구 개설 이벤트를 받으면 공구를 조회해 개설 알림을 발송한다`() {
        val groupBuy = GroupBuyFixture.createGroupBuy(id = 30L, status = GroupBuyStatus.IN_PROGRESS)
        `when`(groupBuyRepository.findWithStoreById(30L)).thenReturn(Optional.of(groupBuy))

        listener.handle(AdminGroupBuyRequestActionService.AdminGroupBuyOpenedEvent(30L))

        verify(groupBuyOpenRequestService).notifyOpened(groupBuy)
    }

    @Test
    fun `공구가 없으면 개설 알림을 발송하지 않는다`() {
        `when`(groupBuyRepository.findWithStoreById(999L)).thenReturn(Optional.empty())

        listener.handle(AdminGroupBuyRequestActionService.AdminGroupBuyOpenedEvent(999L))

        verifyNoInteractions(groupBuyOpenRequestService)
    }
}
