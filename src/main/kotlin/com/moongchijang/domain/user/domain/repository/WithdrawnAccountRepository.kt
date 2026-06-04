package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.WithdrawnAccount
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface WithdrawnAccountRepository : JpaRepository<WithdrawnAccount, Long> {
    fun findByProviderAndIdentifierHash(provider: AuthProvider, identifierHash: String): WithdrawnAccount?

    fun findByWithdrawnUserId(withdrawnUserId: Long): WithdrawnAccount?

    fun findAllByRejoinAvailableAtBefore(rejoinAvailableAt: LocalDateTime): List<WithdrawnAccount>

    fun deleteByRejoinAvailableAtBefore(rejoinAvailableAt: LocalDateTime): Long
}
