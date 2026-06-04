package com.moongchijang.domain.user.application

import com.moongchijang.domain.groupbuy.infrastructure.lock.RedisLockUtil
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class WithdrawalRetentionPurgeScheduler(
    private val withdrawalRetentionPurgeService: WithdrawalRetentionPurgeService,
    private val redisLockUtil: RedisLockUtil,
    @Value("\${withdrawal.retention-purge.lock.wait-ms:100}")
    private val lockWaitMs: Long,
    @Value("\${withdrawal.retention-purge.lock.lease-ms:55000}")
    private val lockLeaseMs: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val running = AtomicBoolean(false)

    @Scheduled(cron = "\${withdrawal.retention-purge.cron:0 30 3 * * *}", zone = "Asia/Seoul")
    fun purgeExpiredRetentionData() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[WithdrawalRetentionPurgeScheduler] 이전 실행이 아직 진행 중이어서 이번 실행을 건너뜁니다.")
            return
        }

        try {
            val token = try {
                redisLockUtil.tryLockOrThrow(RETENTION_PURGE_LOCK_KEY, waitMs = lockWaitMs, leaseMs = lockLeaseMs)
            } catch (e: CustomException) {
                if (e.errorCode == ErrorCode.GROUPBUY_LOCK_ACQUISITION_FAILED) {
                    log.warn("[WithdrawalRetentionPurgeScheduler] 다른 인스턴스에서 실행 중이어서 이번 실행을 건너뜁니다.")
                    return
                }
                throw e
            }

            try {
                withdrawalRetentionPurgeService.purgeExpired()
            } finally {
                val unlocked = redisLockUtil.unlock(RETENTION_PURGE_LOCK_KEY, token)
                if (!unlocked) {
                    log.warn("[WithdrawalRetentionPurgeScheduler] 분산락 해제 실패: key={}", RETENTION_PURGE_LOCK_KEY)
                }
            }
        } finally {
            running.set(false)
        }
    }

    companion object {
        private const val RETENTION_PURGE_LOCK_KEY = "withdrawal:retention-purge"
    }
}
