package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.domain.repository.WithdrawnAccountRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnParticipationRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnPaymentOrderRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnRefundRequestRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class WithdrawalRetentionPurgeServiceTest {

    private val withdrawnAccountRepository: WithdrawnAccountRepository = Mockito.mock(WithdrawnAccountRepository::class.java)
    private val withdrawnPaymentOrderRepository: WithdrawnPaymentOrderRepository = Mockito.mock(WithdrawnPaymentOrderRepository::class.java)
    private val withdrawnParticipationRepository: WithdrawnParticipationRepository = Mockito.mock(WithdrawnParticipationRepository::class.java)
    private val withdrawnRefundRequestRepository: WithdrawnRefundRequestRepository = Mockito.mock(WithdrawnRefundRequestRepository::class.java)

    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-23T03:00:00Z"), ZoneOffset.UTC)

    private val service = WithdrawalRetentionPurgeService(
        withdrawnAccountRepository = withdrawnAccountRepository,
        withdrawnPaymentOrderRepository = withdrawnPaymentOrderRepository,
        withdrawnParticipationRepository = withdrawnParticipationRepository,
        withdrawnRefundRequestRepository = withdrawnRefundRequestRepository,
        clock = clock,
    )

    @Test
    fun `만료 보존 데이터 삭제`() {
        val now = LocalDateTime.of(2026, 6, 4, 3, 30)
        Mockito.`when`(withdrawnRefundRequestRepository.deleteByRetentionExpiresAtBefore(now)).thenReturn(4L)
        Mockito.`when`(withdrawnParticipationRepository.deleteByRetentionExpiresAtBefore(now)).thenReturn(3L)
        Mockito.`when`(withdrawnPaymentOrderRepository.deleteByRetentionExpiresAtBefore(now)).thenReturn(2L)
        Mockito.`when`(withdrawnAccountRepository.deleteByRejoinAvailableAtBefore(now)).thenReturn(1L)

        val result = service.purgeExpired(now)

        assertEquals(1L, result.withdrawnAccountsDeleted)
        assertEquals(2L, result.withdrawnPaymentOrdersDeleted)
        assertEquals(3L, result.withdrawnParticipationsDeleted)
        assertEquals(4L, result.withdrawnRefundRequestsDeleted)
    }
}
