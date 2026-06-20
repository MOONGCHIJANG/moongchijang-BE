package com.moongchijang.domain.payment.domain.repository

import com.moongchijang.domain.payment.domain.entity.PaymentOrder
import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ParticipationPaymentSummary {
    val participationId: Long
    val groupBuyId: Long
    val orderStatus: PaymentOrderStatus
    val paidAt: LocalDateTime?
    val paymentMethod: String?
}

interface PaymentOrderRepository : JpaRepository<PaymentOrder, Long> {
    fun findByOrderId(orderId: String): PaymentOrder?

    fun findAllByUserId(userId: Long): List<PaymentOrder>

    fun findByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): PaymentOrder?

    fun findByUserIdAndGroupBuyIdAndStatus(
        userId: Long,
        groupBuyId: Long,
        status: PaymentOrderStatus
    ): PaymentOrder?

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
    @Query(
        """
        select po
        from PaymentOrder po
        where po.user.id = :userId
          and po.groupBuy.id = :groupBuyId
          and po.status = :status
        """
    )
    fun findByUserIdAndGroupBuyIdAndStatusForUpdate(
        @Param("userId") userId: Long,
        @Param("groupBuyId") groupBuyId: Long,
        @Param("status") status: PaymentOrderStatus
    ): PaymentOrder?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select po from PaymentOrder po where po.orderId = :orderId")
    fun findByOrderIdForUpdate(@Param("orderId") orderId: String): PaymentOrder?

    @Query(
        """
        select p.id as participationId,
               po.groupBuy.id as groupBuyId,
               po.status as orderStatus,
               coalesce(pay.approvedAt, po.approvedAt) as paidAt,
               pay.method as paymentMethod
        from PaymentOrder po
        left join Payment pay on pay.paymentOrder = po
        join Participation p on p.user = po.user and p.groupBuy = po.groupBuy
        where p.id in :participationIds
        """
    )
    fun findPaymentSummariesByParticipationIdIn(
        @Param("participationIds") participationIds: Collection<Long>
    ): List<ParticipationPaymentSummary>
}
