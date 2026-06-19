package com.moongchijang.domain.payment.application

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class PaymentSyntheticMetricsSchedulerTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val paymentMetricsRecorder = PaymentMetricsRecorder(meterRegistry)

    @Test
    fun `synthetic metric은 결제 도메인 counter와 PortOne timer만 기록한다`() {
        val scheduler = PaymentSyntheticMetricsScheduler(
            paymentMetricsRecorder = paymentMetricsRecorder,
            properties = PaymentSyntheticMetricsProperties(
                orderSuccessCount = 2,
                orderFailureCount = 1,
                approvalSuccessCount = 2,
                approvalFailureCount = 1,
                webhookSuccessCount = 1,
                webhookFailureCount = 1,
                refundSuccessCount = 1,
                refundFailureCount = 1,
                cancelSuccessCount = 1,
                portoneSuccessCount = 2,
                portoneFailureCount = 1,
                portoneSuccessLatencyMs = 200,
                portoneFailureLatencyMs = 900,
            ),
        )

        scheduler.recordSamples()

        assertCounter("mcj_payment_order_created_total", 2.0, "result", "success", "reason", "none")
        assertCounter("mcj_payment_order_created_total", 1.0, "result", "failure", "reason", "payment_invalid_quantity")
        assertCounter(
            "mcj_payment_approval_total",
            2.0,
            "source", "complete_api",
            "result", "success",
            "reason", "none",
        )
        assertCounter(
            "mcj_payment_approval_total",
            1.0,
            "source", "complete_api",
            "result", "failure",
            "reason", "payment_order_not_found",
        )
        assertCounter(
            "mcj_payment_webhook_processed_total",
            1.0,
            "event_type", "transaction_paid",
            "result", "success",
            "reason", "none",
        )
        assertCounter(
            "mcj_payment_webhook_processed_total",
            1.0,
            "event_type", "transaction_failed",
            "result", "failure",
            "reason", "failed",
        )
        assertCounter("mcj_payment_refund_processed_total", 1.0, "result", "success", "reason", "none")
        assertCounter(
            "mcj_payment_refund_processed_total",
            1.0,
            "result", "failure",
            "reason", "payment_cancel_failed",
        )
        assertCounter(
            "mcj_payment_cancel_total",
            1.0,
            "source", "user",
            "result", "success",
            "reason", "none",
            "partial", "false",
        )
        assertCounter(
            "mcj_portone_api_requests_total",
            2.0,
            "operation", "get_payment",
            "result", "success",
            "status", "paid",
        )
        assertCounter(
            "mcj_portone_api_requests_total",
            1.0,
            "operation", "get_payment",
            "result", "failure",
            "status", "portone_unexpected_status",
        )

        val timer = meterRegistry.find("mcj_portone_api_latency_seconds")
            .tags("operation", "get_payment", "result", "success", "status", "paid")
            .timer()

        assertEquals(2, timer?.count())
        assertEquals(400.0, timer?.totalTime(TimeUnit.MILLISECONDS))
    }

    private fun assertCounter(name: String, expected: Double, vararg tags: String) {
        val counter = meterRegistry.find(name).tags(*tags).counter()

        assertEquals(expected, counter?.count())
    }
}
