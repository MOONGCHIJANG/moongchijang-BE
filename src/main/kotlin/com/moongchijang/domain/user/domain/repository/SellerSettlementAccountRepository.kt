package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.SellerSettlementAccount
import org.springframework.data.jpa.repository.JpaRepository

interface SellerSettlementAccountRepository : JpaRepository<SellerSettlementAccount, Long> {
    fun findByUserId(userId: Long): SellerSettlementAccount?
    fun deleteByUserId(userId: Long): Long
}
