package com.moongchijang.domain.payment.application

import com.moongchijang.domain.notification.application.discord.AdminDiscordAlertService
import com.moongchijang.domain.payment.domain.entity.PaymentAuditEventType
import com.moongchijang.domain.payment.domain.entity.PaymentAuditLog
import com.moongchijang.domain.payment.domain.entity.PaymentAuditSource
import com.moongchijang.domain.payment.domain.entity.PaymentOrder
import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import com.moongchijang.domain.payment.domain.repository.PaymentAuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class PaymentAuditLogService(
    private val paymentAuditLogRepository: PaymentAuditLogRepository,
    private val adminDiscordAlertService: AdminDiscordAlertService,
    transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun record(record: PaymentAuditRecord) {
        try {
            transactionTemplate.execute {
                paymentAuditLogRepository.save(
                    PaymentAuditLog(
                        paymentOrder = record.paymentOrder,
                        orderId = record.orderId ?: record.paymentOrder?.orderId,
                        pgPaymentId = record.pgPaymentId,
                        eventType = record.eventType,
                        source = record.source,
                        previousOrderStatus = record.previousOrderStatus,
                        currentOrderStatus = record.currentOrderStatus ?: record.paymentOrder?.status,
                        pgStatus = record.pgStatus,
                        reason = record.reason?.take(500),
                        rawPayload = record.rawPayload,
                    )
                )
            }
        } catch (e: Exception) {
            log.warn(
                "[PaymentAuditLogService] 결제 감사 이력 저장 실패: source={}, eventType={}, orderId={}",
                record.source,
                record.eventType,
                record.orderId ?: record.paymentOrder?.orderId,
                e
            )
        }

        if (record.notifyFailure) {
            adminDiscordAlertService.sendPaymentFailed(
                orderId = record.orderId ?: record.paymentOrder?.orderId,
                pgPaymentId = record.pgPaymentId,
                pgStatus = record.pgStatus,
                reason = record.reason,
            )
        }
    }
}

data class PaymentAuditRecord(
    val source: PaymentAuditSource,
    val eventType: PaymentAuditEventType,
    val paymentOrder: PaymentOrder? = null,
    val orderId: String? = null,
    val pgPaymentId: String? = null,
    val previousOrderStatus: PaymentOrderStatus? = null,
    val currentOrderStatus: PaymentOrderStatus? = null,
    val pgStatus: String? = null,
    val reason: String? = null,
    val rawPayload: String? = null,
    val notifyFailure: Boolean = false,
)
