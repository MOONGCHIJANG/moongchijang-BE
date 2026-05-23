package com.moongchijang.domain.payment.application

import com.moongchijang.domain.groupbuy.infrastructure.lock.RedisLockUtil
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Mock

@ExtendWith(MockitoExtension::class)
class PendingRefundSchedulerTest {

    @Mock
    private lateinit var paymentService: PaymentService

    @Mock
    private lateinit var redisLockUtil: RedisLockUtil

    @Test
    fun `분산락 획득 성공 시 환불대기 처리를 실행하고 락을 해제한다`() {
        val scheduler = PendingRefundScheduler(
            paymentService = paymentService,
            redisLockUtil = redisLockUtil,
            batchSize = 100,
            lockWaitMs = 100,
            lockLeaseMs = 55_000,
        )
        `when`(redisLockUtil.tryLockOrThrow("payment:pending-refund", 100, 55_000)).thenReturn("lock-token")
        `when`(paymentService.processPendingRefunds(100))
            .thenReturn(PendingRefundProcessingResult(targetCount = 1, successCount = 1, failedCount = 0))

        scheduler.processPendingRefunds()

        verify(paymentService).processPendingRefunds(100)
        verify(redisLockUtil).unlock("payment:pending-refund", "lock-token")
    }

    @Test
    fun `분산락 획득 실패 시 환불대기 처리를 건너뛴다`() {
        val scheduler = PendingRefundScheduler(
            paymentService = paymentService,
            redisLockUtil = redisLockUtil,
            batchSize = 100,
            lockWaitMs = 100,
            lockLeaseMs = 55_000,
        )
        `when`(redisLockUtil.tryLockOrThrow("payment:pending-refund", 100, 55_000))
            .thenThrow(CustomException(ErrorCode.GROUPBUY_LOCK_ACQUISITION_FAILED))

        scheduler.processPendingRefunds()

        verify(paymentService, never()).processPendingRefunds(100)
        verify(redisLockUtil, never()).unlock("payment:pending-refund", "lock-token")
    }
}
