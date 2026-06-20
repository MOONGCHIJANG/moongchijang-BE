package com.moongchijang.domain.payment.application

import com.moongchijang.domain.notification.application.discord.AdminDiscordAlertService
import com.moongchijang.domain.notification.infrastructure.discord.DiscordProperties
import com.moongchijang.domain.payment.domain.entity.PaymentAuditEventType
import com.moongchijang.domain.payment.domain.entity.PaymentAuditSource
import com.moongchijang.domain.payment.domain.repository.PaymentAuditLogRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.SimpleTransactionStatus

class PaymentAuditLogServiceTest {

    @Test
    fun `결제 성공 알림 설정이 켜져 있으면 성공 감사 이력 기록 후 디스코드 알림을 발송한다`() {
        val repository = mock(PaymentAuditLogRepository::class.java)
        val alertService = mock(AdminDiscordAlertService::class.java)
        val transactionManager = stubTransactionManager()
        val meterRegistry = SimpleMeterRegistry()
        val service = PaymentAuditLogService(
            paymentAuditLogRepository = repository,
            adminDiscordAlertService = alertService,
            discordProperties = DiscordProperties(paymentSuccessAlertEnabled = true),
            paymentMetricsRecorder = PaymentMetricsRecorder(meterRegistry),
            transactionManager = transactionManager,
        )

        service.record(successRecord())

        verify(repository).save(any())
        verify(alertService).sendPaymentSucceeded(
            orderId = "MCJ-10-test",
            pgPaymentId = "portone-payment-id",
            amount = 12000,
            method = "CARD",
        )
        assertCounter(
            meterRegistry,
            "mcj_payment_audit_events_total",
            1.0,
            "source", "complete_api",
            "event_type", "payment_approved",
            "result", "success",
            "reason", "paid",
        )
    }

    @Test
    fun `결제 성공 알림 설정이 꺼져 있으면 성공 감사 이력만 기록하고 디스코드 알림은 발송하지 않는다`() {
        val repository = mock(PaymentAuditLogRepository::class.java)
        val alertService = mock(AdminDiscordAlertService::class.java)
        val transactionManager = stubTransactionManager()
        val meterRegistry = SimpleMeterRegistry()
        val service = PaymentAuditLogService(
            paymentAuditLogRepository = repository,
            adminDiscordAlertService = alertService,
            discordProperties = DiscordProperties(paymentSuccessAlertEnabled = false),
            paymentMetricsRecorder = PaymentMetricsRecorder(meterRegistry),
            transactionManager = transactionManager,
        )

        service.record(successRecord())

        verify(repository).save(any())
        verify(alertService, never()).sendPaymentSucceeded(
            orderId = "MCJ-10-test",
            pgPaymentId = "portone-payment-id",
            amount = 12000,
            method = "CARD",
        )
    }

    @Test
    fun `감사 이력 저장 실패 metric은 pgStatus를 reason fallback으로 기록한다`() {
        val repository = mock(PaymentAuditLogRepository::class.java)
        val alertService = mock(AdminDiscordAlertService::class.java)
        val transactionManager = stubTransactionManager()
        val meterRegistry = SimpleMeterRegistry()
        val service = PaymentAuditLogService(
            paymentAuditLogRepository = repository,
            adminDiscordAlertService = alertService,
            discordProperties = DiscordProperties(paymentSuccessAlertEnabled = false),
            paymentMetricsRecorder = PaymentMetricsRecorder(meterRegistry),
            transactionManager = transactionManager,
        )
        `when`(repository.save(any())).thenThrow(IllegalStateException("db unavailable"))

        service.record(successRecord())

        assertCounter(
            meterRegistry,
            "mcj_payment_audit_events_total",
            1.0,
            "source", "complete_api",
            "event_type", "payment_approved",
            "result", "audit_record_failure",
            "reason", "paid",
        )
    }

    @Test
    fun `주문 생성 실패 감사 이력은 order create failure metric으로 기록한다`() {
        val repository = mock(PaymentAuditLogRepository::class.java)
        val alertService = mock(AdminDiscordAlertService::class.java)
        val transactionManager = stubTransactionManager()
        val meterRegistry = SimpleMeterRegistry()
        val service = PaymentAuditLogService(
            paymentAuditLogRepository = repository,
            adminDiscordAlertService = alertService,
            discordProperties = DiscordProperties(paymentSuccessAlertEnabled = false),
            paymentMetricsRecorder = PaymentMetricsRecorder(meterRegistry),
            transactionManager = transactionManager,
        )

        service.record(
            PaymentAuditRecord(
                source = PaymentAuditSource.ORDER_CREATE,
                eventType = PaymentAuditEventType.ORDER_CREATE_FAILED,
                reason = "PAYMENT_DUPLICATE_PARTICIPATION",
                rawPayload = "groupBuyId=10,userId=1,quantity=1",
            )
        )

        verify(repository).save(any())
        assertCounter(
            meterRegistry,
            "mcj_payment_audit_events_total",
            1.0,
            "source", "order_create",
            "event_type", "order_create_failed",
            "result", "failure",
            "reason", "payment_duplicate_participation",
        )
    }

    private fun successRecord(): PaymentAuditRecord =
        PaymentAuditRecord(
            source = PaymentAuditSource.COMPLETE_API,
            eventType = PaymentAuditEventType.PAYMENT_APPROVED,
            orderId = "MCJ-10-test",
            pgPaymentId = "portone-payment-id",
            pgStatus = "PAID",
            amount = 12000,
            method = "CARD",
            notifySuccess = true,
        )

    private fun assertCounter(
        meterRegistry: SimpleMeterRegistry,
        name: String,
        expected: Double,
        vararg tags: String,
    ) {
        val counter = meterRegistry.find(name).tags(*tags).counter()

        assertEquals(expected, counter?.count())
    }

    private fun stubTransactionManager(): PlatformTransactionManager {
        val transactionManager = mock(PlatformTransactionManager::class.java)
        `when`(transactionManager.getTransaction(any(TransactionDefinition::class.java)))
            .thenReturn(SimpleTransactionStatus())
        return transactionManager
    }
}
