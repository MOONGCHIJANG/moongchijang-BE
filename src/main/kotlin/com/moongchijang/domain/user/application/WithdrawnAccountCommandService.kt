package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.WithdrawnAccount
import com.moongchijang.domain.user.domain.repository.WithdrawnAccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WithdrawnAccountCommandService(
    private val withdrawnAccountRepository: WithdrawnAccountRepository,
    private val withdrawalIdentifierHasher: WithdrawalIdentifierHasher,
) {
    @Transactional
    fun recordWithdrawal(user: User, withdrawnAt: LocalDateTime) {
        val identifierHash = withdrawalIdentifierHasher.hashForWithdrawal(
            provider = user.provider,
            providerId = user.providerId,
            email = user.email,
        )
        val existing = findExisting(user)

        val withdrawnAccount = existing?.apply {
            provider = user.provider
            this.identifierHash = requireNotNull(identifierHash)
            withdrawnUserId = requireNotNull(user.id)
            this.withdrawnAt = withdrawnAt
            rejoinAvailableAt = withdrawnAt.plusDays(REJOIN_WAIT_DAYS)
        } ?: WithdrawnAccount(
            provider = user.provider,
            identifierHash = requireNotNull(identifierHash),
            withdrawnUserId = requireNotNull(user.id),
            withdrawnAt = withdrawnAt,
            rejoinAvailableAt = withdrawnAt.plusDays(REJOIN_WAIT_DAYS),
        )

        withdrawnAccountRepository.save(withdrawnAccount)
    }

    private fun findExisting(user: User): WithdrawnAccount? {
        val identifierHash = withdrawalIdentifierHasher.hashForWithdrawal(
            provider = user.provider,
            providerId = user.providerId,
            email = user.email,
        )
        if (!identifierHash.isNullOrBlank()) {
            return withdrawnAccountRepository.findByProviderAndIdentifierHash(user.provider, identifierHash)
        }

        return withdrawnAccountRepository.findByWithdrawnUserId(requireNotNull(user.id))
    }

    companion object {
        private const val REJOIN_WAIT_DAYS = 30L
    }
}
