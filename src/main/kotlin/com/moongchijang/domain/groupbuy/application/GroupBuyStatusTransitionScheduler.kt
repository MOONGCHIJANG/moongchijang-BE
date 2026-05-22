package com.moongchijang.domain.groupbuy.application

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class GroupBuyStatusTransitionScheduler(
    private val groupBuyStatusTransitionService: GroupBuyStatusTransitionService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${groupbuy.status-transition.fixed-delay-ms:60000}")
    fun transitionExpiredGroupBuys() {
        log.debug("[GroupBuyStatusTransitionScheduler] deadline 자동 전이 스케줄 실행")
        groupBuyStatusTransitionService.transitionExpiredGroupBuys()
    }
}
