package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.domain.repository.WithdrawnAccountRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnParticipationRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnPaymentOrderRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnRefundRequestRepository
import com.moongchijang.global.time.utcNow
import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class WithdrawalRetentionPurgeResult(
    val withdrawnAccountsDeleted: Long,
    val withdrawnPaymentOrdersDeleted: Long,
    val withdrawnParticipationsDeleted: Long,
    val withdrawnRefundRequestsDeleted: Long,
)

@Service
class WithdrawalRetentionPurgeService(
    private val withdrawnAccountRepository: WithdrawnAccountRepository,
    private val withdrawnPaymentOrderRepository: WithdrawnPaymentOrderRepository,
    private val withdrawnParticipationRepository: WithdrawnParticipationRepository,
    private val withdrawnRefundRequestRepository: WithdrawnRefundRequestRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun purgeExpired(): WithdrawalRetentionPurgeResult = purgeExpired(clock.utcNow())

    @Transactional
    fun purgeExpired(now: LocalDateTime): WithdrawalRetentionPurgeResult {
        val withdrawnRefundRequestsDeleted = withdrawnRefundRequestRepository.deleteByRetentionExpiresAtBefore(now)
        val withdrawnParticipationsDeleted = withdrawnParticipationRepository.deleteByRetentionExpiresAtBefore(now)
        val withdrawnPaymentOrdersDeleted = withdrawnPaymentOrderRepository.deleteByRetentionExpiresAtBefore(now)
        val withdrawnAccountsDeleted = withdrawnAccountRepository.deleteByRejoinAvailableAtBefore(now)

        val result = WithdrawalRetentionPurgeResult(
            withdrawnAccountsDeleted = withdrawnAccountsDeleted,
            withdrawnPaymentOrdersDeleted = withdrawnPaymentOrdersDeleted,
            withdrawnParticipationsDeleted = withdrawnParticipationsDeleted,
            withdrawnRefundRequestsDeleted = withdrawnRefundRequestsDeleted,
        )

        if (
            result.withdrawnAccountsDeleted > 0 ||
            result.withdrawnPaymentOrdersDeleted > 0 ||
            result.withdrawnParticipationsDeleted > 0 ||
            result.withdrawnRefundRequestsDeleted > 0
        ) {
            log.info(
                "[WithdrawalRetentionPurgeService] 만료 보존 데이터 삭제 완료: withdrawnAccountsDeleted={}, withdrawnPaymentOrdersDeleted={}, withdrawnParticipationsDeleted={}, withdrawnRefundRequestsDeleted={}",
                result.withdrawnAccountsDeleted,
                result.withdrawnPaymentOrdersDeleted,
                result.withdrawnParticipationsDeleted,
                result.withdrawnRefundRequestsDeleted,
            )
        }

        return result
    }
}
