package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.infrastructure.lock.RedisLockUtil
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class GroupBuyStatusTransitionScheduler(
    private val groupBuyStatusTransitionService: GroupBuyStatusTransitionService,
    private val redisLockUtil: RedisLockUtil,
    @Value("\${groupbuy.status-transition.lock.wait-ms:100}")
    private val lockWaitMs: Long,
    @Value("\${groupbuy.status-transition.lock.lease-ms:55000}")
    private val lockLeaseMs: Long
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
        val key = STATUS_TRANSITION_LOCK_KEY
        val token = try {
            redisLockUtil.tryLockOrThrow(key, waitMs = lockWaitMs, leaseMs = lockLeaseMs)
        } catch (e: CustomException) {
            if (e.errorCode == ErrorCode.GROUPBUY_LOCK_ACQUISITION_FAILED) {
                log.warn("[GroupBuyStatusTransitionScheduler] 다른 인스턴스에서 실행 중이어서 이번 실행을 건너뜁니다.")
                running.set(false)
                return
            }
            running.set(false)
            throw e
        }

        try {
            groupBuyStatusTransitionService.transitionExpiredGroupBuys()
        } finally {
            val unlocked = redisLockUtil.unlock(key, token)
            if (!unlocked) {
                log.warn("[GroupBuyStatusTransitionScheduler] 분산락 해제 실패: key={}", key)
            }
            running.set(false)
        }
    }

    companion object {
        private const val STATUS_TRANSITION_LOCK_KEY = "groupBuy:status-transition"
    }
}
