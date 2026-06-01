package com.moongchijang.domain.payment.application

import com.moongchijang.domain.groupbuy.infrastructure.lock.RedisLockUtil
import com.moongchijang.domain.notification.application.discord.AdminDiscordAlertService
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PendingRefundScheduler(
    private val paymentService: PaymentService,
    private val redisLockUtil: RedisLockUtil,
    private val adminDiscordAlertService: AdminDiscordAlertService,
    @Value("\${payment.pending-refund.batch-size:100}")
    private val batchSize: Int,
    @Value("\${payment.pending-refund.lock.wait-ms:100}")
    private val lockWaitMs: Long,
    @Value("\${payment.pending-refund.lock.lease-ms:55000}")
    private val lockLeaseMs: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${payment.pending-refund.fixed-delay-ms:60000}")
    fun processPendingRefunds() {
        val token = try {
            redisLockUtil.tryLockOrThrow(PENDING_REFUND_LOCK_KEY, waitMs = lockWaitMs, leaseMs = lockLeaseMs)
        } catch (e: CustomException) {
            if (e.errorCode == ErrorCode.GROUPBUY_LOCK_ACQUISITION_FAILED) {
                log.warn("[PendingRefundScheduler] 다른 인스턴스에서 실행 중이어서 이번 실행을 건너뜁니다.")
                return
            }
            throw e
        }

        try {
            val result = paymentService.processPendingRefunds(batchSize)
            if (result.targetCount > 0) {
                log.info(
                    "[PendingRefundScheduler] 환불대기 처리 완료: targetCount={}, successCount={}, failedCount={}",
                    result.targetCount,
                    result.successCount,
                    result.failedCount
                )
                if (result.failedCount > 0) {
                    adminDiscordAlertService.sendRefundFailedSummary(result.failedCount)
                }
            }
        } finally {
            val unlocked = redisLockUtil.unlock(PENDING_REFUND_LOCK_KEY, token)
            if (!unlocked) {
                log.warn("[PendingRefundScheduler] 분산락 해제 실패: key={}", PENDING_REFUND_LOCK_KEY)
            }
        }
    }

    companion object {
        private const val PENDING_REFUND_LOCK_KEY = "payment:pending-refund"
    }
}
