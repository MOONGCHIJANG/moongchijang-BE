package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.SellerSettlementAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SellerSettlementAccountRepository : JpaRepository<SellerSettlementAccount, Long> {
    fun findByUserId(userId: Long): SellerSettlementAccount?
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SellerSettlementAccount a WHERE a.user.id = :userId")
    fun deleteByUserId(@Param("userId") userId: Long): Long
}
