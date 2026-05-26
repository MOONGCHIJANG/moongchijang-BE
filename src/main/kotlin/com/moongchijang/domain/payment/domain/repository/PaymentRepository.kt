package com.moongchijang.domain.payment.domain.repository

import com.moongchijang.domain.payment.domain.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByPaymentOrderOrderId(orderId: String): Payment?

    @Query(
        """
        select p
        from Payment p
        join fetch p.paymentOrder po
        where po.groupBuy.id = :groupBuyId
          and po.user.id in :userIds
        """
    )
    fun findAllByGroupBuyIdAndUserIdIn(
        @Param("groupBuyId") groupBuyId: Long,
        @Param("userIds") userIds: Collection<Long>
    ): List<Payment>
}
