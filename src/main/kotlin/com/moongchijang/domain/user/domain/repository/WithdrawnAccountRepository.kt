package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.WithdrawnAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface WithdrawnAccountRepository : JpaRepository<WithdrawnAccount, Long> {
    fun findByProviderAndIdentifierHash(provider: AuthProvider, identifierHash: String): WithdrawnAccount?

    fun findByWithdrawnUserId(withdrawnUserId: Long): WithdrawnAccount?

    fun findAllByRejoinAvailableAtBefore(rejoinAvailableAt: LocalDateTime): List<WithdrawnAccount>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM WithdrawnAccount a WHERE a.rejoinAvailableAt < :rejoinAvailableAt")
    fun deleteByRejoinAvailableAtBefore(@Param("rejoinAvailableAt") rejoinAvailableAt: LocalDateTime): Long
}
