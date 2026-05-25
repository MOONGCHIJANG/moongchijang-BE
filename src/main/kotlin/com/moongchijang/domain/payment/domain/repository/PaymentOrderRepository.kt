package com.moongchijang.domain.payment.domain.repository

import com.moongchijang.domain.payment.domain.entity.PaymentOrder
import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PaymentOrderRepository : JpaRepository<PaymentOrder, Long> {
    fun findByOrderId(orderId: String): PaymentOrder?

    fun findByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): PaymentOrder?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select po
        from PaymentOrder po
        where po.user.id = :userId
          and po.groupBuy.id = :groupBuyId
        """
    )
    fun findByUserIdAndGroupBuyIdForUpdate(
        @Param("userId") userId: Long,
        @Param("groupBuyId") groupBuyId: Long
    ): PaymentOrder?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select po from PaymentOrder po where po.orderId = :orderId")
    fun findByOrderIdForUpdate(@Param("orderId") orderId: String): PaymentOrder?

    @Query(
        """
        select po.groupBuy.id
        from PaymentOrder po
        where po.user.id = :userId
          and po.status = :status
        """
    )
    fun findGroupBuyIdsByUserIdAndStatus(
        @Param("userId") userId: Long,
        @Param("status") status: PaymentOrderStatus,
    ): List<Long>
}
