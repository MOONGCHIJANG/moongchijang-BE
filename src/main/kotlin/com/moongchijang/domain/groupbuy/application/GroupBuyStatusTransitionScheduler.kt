package com.moongchijang.domain.groupbuy.application

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class GroupBuyStatusTransitionScheduler(
    private val groupBuyStatusTransitionService: GroupBuyStatusTransitionService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val running = AtomicBoolean(false)

    @Scheduled(fixedDelayString = "\${groupbuy.status-transition.fixed-delay-ms:60000}")
    fun transitionExpiredGroupBuys() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[GroupBuyStatusTransitionScheduler] 이전 실행이 아직 진행 중이어서 이번 실행을 건너뜁니다.")
            return
        }

        log.debug("[GroupBuyStatusTransitionScheduler] deadline 자동 전이 스케줄 실행")
        try {
            groupBuyStatusTransitionService.transitionExpiredGroupBuys()
        } finally {
            running.set(false)
        }
    }
}
