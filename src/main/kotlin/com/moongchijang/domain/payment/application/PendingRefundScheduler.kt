package com.moongchijang.domain.payment.application

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class PendingRefundScheduler(
    private val paymentService: PaymentService,
    @Value("\${payment.pending-refund.batch-size:100}")
    private val batchSize: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val running = AtomicBoolean(false)

    @Scheduled(fixedDelayString = "\${payment.pending-refund.fixed-delay-ms:60000}")
    fun processPendingRefunds() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[PendingRefundScheduler] 이전 실행이 아직 진행 중이어서 이번 실행을 건너뜁니다.")
            return
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
            }
        } finally {
            running.set(false)
        }
    }
}
