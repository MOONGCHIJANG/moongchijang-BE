package com.moongchijang.domain.payment.application

import java.util.concurrent.TimeUnit
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("dev", "local", "local-demo")
@ConditionalOnProperty(prefix = "monitoring.payment.synthetic", name = ["enabled"], havingValue = "true")
class PaymentSyntheticMetricsScheduler(
    private val paymentMetricsRecorder: PaymentMetricsRecorder,
    private val properties: PaymentSyntheticMetricsProperties,
) {
    @Scheduled(
        fixedDelayString = "\${monitoring.payment.synthetic.fixed-delay-ms:60000}",
        initialDelayString = "\${monitoring.payment.synthetic.initial-delay-ms:10000}",
    )
    fun recordSamples() {
        repeatPositive(properties.orderSuccessCount) {
            paymentMetricsRecorder.recordOrderCreated(result = "success")
        }
        repeatPositive(properties.orderFailureCount) {
            paymentMetricsRecorder.recordOrderCreated(result = "failure", reason = "PAYMENT_INVALID_QUANTITY")
        }
        repeatPositive(properties.approvalSuccessCount) {
            paymentMetricsRecorder.recordApproval(source = "complete_api", result = "success")
        }
        repeatPositive(properties.approvalFailureCount) {
            paymentMetricsRecorder.recordApproval(
                source = "complete_api",
                result = "failure",
                reason = "PAYMENT_ORDER_NOT_FOUND",
            )
        }
        repeatPositive(properties.webhookSuccessCount) {
            paymentMetricsRecorder.recordWebhook(eventType = "Transaction.Paid", result = "success")
        }
        repeatPositive(properties.webhookFailureCount) {
            paymentMetricsRecorder.recordWebhook(
                eventType = "Transaction.Failed",
                result = "failure",
                reason = "FAILED",
            )
        }
        repeatPositive(properties.refundSuccessCount) {
            paymentMetricsRecorder.recordRefund(result = "success")
        }
        repeatPositive(properties.refundFailureCount) {
            paymentMetricsRecorder.recordRefund(result = "failure", reason = "PAYMENT_CANCEL_FAILED")
        }
        repeatPositive(properties.cancelSuccessCount) {
            paymentMetricsRecorder.recordCancel(source = "user", result = "success", partial = false)
        }
        repeatPositive(properties.portoneSuccessCount) {
            paymentMetricsRecorder.recordPortOneRequest(
                operation = "get_payment",
                result = "success",
                status = "PAID",
                elapsedNanos = TimeUnit.MILLISECONDS.toNanos(properties.portoneSuccessLatencyMs.coerceAtLeast(0)),
            )
        }
        repeatPositive(properties.portoneFailureCount) {
            paymentMetricsRecorder.recordPortOneRequest(
                operation = "get_payment",
                result = "failure",
                status = "PORTONE_UNEXPECTED_STATUS",
                elapsedNanos = TimeUnit.MILLISECONDS.toNanos(properties.portoneFailureLatencyMs.coerceAtLeast(0)),
            )
        }
    }

    private fun repeatPositive(times: Int, action: () -> Unit) {
        repeat(times.coerceAtLeast(0)) {
            action()
        }
    }
}
