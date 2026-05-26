package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AdminGroupBuyOpenedEventListener(
    private val groupBuyRepository: GroupBuyRepository,
    private val groupBuyOpenRequestService: GroupBuyOpenRequestService,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: AdminGroupBuyRequestActionService.AdminGroupBuyOpenedEvent) {
        groupBuyRepository.findWithStoreById(event.groupBuyId)
            .ifPresent { groupBuyOpenRequestService.notifyOpened(it) }
    }
}
