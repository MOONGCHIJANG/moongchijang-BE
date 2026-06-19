package com.moongchijang.domain.payment.application

import com.moongchijang.domain.notification.application.discord.AdminDiscordAlertService
import com.moongchijang.domain.notification.infrastructure.discord.DiscordProperties
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
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class PaymentAuditLogService(
    private val paymentAuditLogRepository: PaymentAuditLogRepository,
    private val adminDiscordAlertService: AdminDiscordAlertService,
    private val discordProperties: DiscordProperties,
    private val paymentMetricsRecorder: PaymentMetricsRecorder,
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
            val result = record.eventType.toAuditMetricResult()
            paymentMetricsRecorder.recordAuditEvent(
                source = record.source.name,
                eventType = record.eventType.name,
                result = result,
                reason = record.reason ?: record.pgStatus ?: NONE,
            )
            log.info(
                "[payment_audit] source={} eventType={} result={} orderId={} pgPaymentId={} pgStatus={} previousOrderStatus={} currentOrderStatus={} reason={}",
                record.source,
                record.eventType,
                result,
                record.orderId ?: record.paymentOrder?.orderId,
                record.pgPaymentId,
                record.pgStatus,
                record.previousOrderStatus,
                record.currentOrderStatus ?: record.paymentOrder?.status,
                record.reason,
            )
        } catch (e: Exception) {
            paymentMetricsRecorder.recordAuditEvent(
                source = record.source.name,
                eventType = record.eventType.name,
                result = "audit_record_failure",
                reason = record.reason ?: NONE,
            )
            log.warn(
                "[PaymentAuditLogService] 결제 감사 이력 저장 실패: source={}, eventType={}, orderId={}",
                record.source,
                record.eventType,
                record.orderId ?: record.paymentOrder?.orderId,
                e
            )
        }

        if (record.notifyFailure) {
            try {
                adminDiscordAlertService.sendPaymentFailed(
                    orderId = record.orderId ?: record.paymentOrder?.orderId,
                    pgPaymentId = record.pgPaymentId,
                    pgStatus = record.pgStatus,
                    reason = record.reason,
                )
            } catch (e: Exception) {
                log.warn(
                    "[PaymentAuditLogService] 결제 실패 Discord 알림 발행 실패: orderId={}",
                    record.orderId ?: record.paymentOrder?.orderId,
                    e
                )
            }
        }

        if (record.notifySuccess && discordProperties.paymentSuccessAlertEnabled) {
            val sendNotification = {
                sendPaymentSuccessAlert(record)
            }
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                    object : TransactionSynchronization {
                        override fun afterCommit() {
                            sendNotification()
                        }
                    }
                )
            } else {
                sendNotification()
            }
        }
    }

    private fun sendPaymentSuccessAlert(record: PaymentAuditRecord) {
        try {
            adminDiscordAlertService.sendPaymentSucceeded(
                orderId = record.orderId ?: record.paymentOrder?.orderId,
                pgPaymentId = record.pgPaymentId,
                amount = record.amount,
                method = record.method,
            )
        } catch (e: Exception) {
            log.warn(
                "[PaymentAuditLogService] 결제 성공 Discord 알림 발행 실패: orderId={}",
                record.orderId ?: record.paymentOrder?.orderId,
                e
            )
        }
    }

    private fun PaymentAuditEventType.toAuditMetricResult(): String =
        when (this) {
            PaymentAuditEventType.PAYMENT_APPROVED,
            PaymentAuditEventType.PAYMENT_CANCELLED,
            PaymentAuditEventType.PAYMENT_PARTIAL_CANCELLED,
            -> "success"
            PaymentAuditEventType.PAYMENT_FAILED -> "failure"
            PaymentAuditEventType.PAYMENT_IGNORED -> "ignored"
            PaymentAuditEventType.COMPLETE_REQUEST_RECEIVED,
            PaymentAuditEventType.WEBHOOK_RECEIVED,
            PaymentAuditEventType.PORTONE_STATUS_FETCHED,
            -> "event"
        }

    companion object {
        private const val NONE = "none"
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
    val amount: Int? = null,
    val method: String? = null,
    val reason: String? = null,
    val rawPayload: String? = null,
    val notifyFailure: Boolean = false,
    val notifySuccess: Boolean = false,
)
