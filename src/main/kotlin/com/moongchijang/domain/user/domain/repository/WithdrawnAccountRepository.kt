package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.WithdrawnAccount
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface WithdrawnAccountRepository : JpaRepository<WithdrawnAccount, Long> {
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): WithdrawnAccount?

    fun findByProviderAndEmail(provider: AuthProvider, email: String): WithdrawnAccount?

    fun findByWithdrawnUserId(withdrawnUserId: Long): WithdrawnAccount?

    fun findAllByRejoinAvailableAtBefore(rejoinAvailableAt: LocalDateTime): List<WithdrawnAccount>

    fun deleteByRejoinAvailableAtBefore(rejoinAvailableAt: LocalDateTime): Long
}
