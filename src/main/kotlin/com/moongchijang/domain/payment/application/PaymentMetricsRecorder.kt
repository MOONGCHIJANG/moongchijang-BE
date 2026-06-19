package com.moongchijang.domain.payment.application

import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.TimeUnit
import org.springframework.stereotype.Component

@Component
class PaymentMetricsRecorder(
    private val meterRegistry: MeterRegistry,
) {
    fun recordOrderCreated(result: String, reason: String = NONE) {
        counter(
            "mcj_payment_order_created_total",
            "result", result,
            "reason", normalizeReason(reason),
        ).increment()
    }

    fun recordApproval(source: String, result: String, reason: String = NONE) {
        counter(
            "mcj_payment_approval_total",
            "source", normalizeSource(source),
            "result", result,
            "reason", normalizeReason(reason),
        ).increment()
    }

    fun recordCancel(source: String, result: String, reason: String = NONE, partial: Boolean = false) {
        counter(
            "mcj_payment_cancel_total",
            "source", normalizeSource(source),
            "result", result,
            "reason", normalizeReason(reason),
            "partial", partial.toString(),
        ).increment()
    }

    fun recordWebhook(eventType: String?, result: String, reason: String = NONE) {
        counter(
            "mcj_payment_webhook_processed_total",
            "event_type", normalizeWebhookEventType(eventType),
            "result", result,
            "reason", normalizeReason(reason),
        ).increment()
    }

    fun recordRefund(result: String, reason: String = NONE) {
        counter(
            "mcj_payment_refund_processed_total",
            "result", result,
            "reason", normalizeReason(reason),
        ).increment()
    }

    fun recordPortOneRequest(operation: String, result: String, status: String, elapsedNanos: Long) {
        val normalizedOperation = normalizeOperation(operation)
        val normalizedStatus = normalizeReason(status)
        counter(
            "mcj_portone_api_requests_total",
            "operation", normalizedOperation,
            "result", result,
            "status", normalizedStatus,
        ).increment()
        meterRegistry.timer(
            "mcj_portone_api_latency_seconds",
            "operation", normalizedOperation,
            "result", result,
            "status", normalizedStatus,
        ).record(elapsedNanos.coerceAtLeast(0), TimeUnit.NANOSECONDS)
    }

    fun recordAuditEvent(source: String, eventType: String, result: String, reason: String = NONE) {
        counter(
            "mcj_payment_audit_events_total",
            "source", normalizeSource(source),
            "event_type", normalizeAuditEventType(eventType),
            "result", normalizeAuditResult(result),
            "reason", normalizeReason(reason),
        ).increment()
    }

    private fun counter(name: String, vararg tags: String) =
        meterRegistry.counter(name, *tags)

    private fun normalizeSource(source: String): String =
        when (source.lowercase()) {
            "complete_api", "webhook", "scheduler", "admin", "user", "cancel_api", "pending_refund" -> source.lowercase()
            else -> "other"
        }

    private fun normalizeOperation(operation: String): String =
        when (operation.lowercase()) {
            "get_payment", "cancel_payment" -> operation.lowercase()
            else -> "other"
        }

    private fun normalizeWebhookEventType(eventType: String?): String =
        when (eventType?.lowercase()) {
            "transaction.paid" -> "transaction_paid"
            "transaction.cancelled", "transaction.canceled" -> "transaction_cancelled"
            "transaction.failed" -> "transaction_failed"
            null, "" -> "unknown"
            else -> "other"
        }

    private fun normalizeAuditEventType(eventType: String): String =
        when (eventType.lowercase()) {
            "complete_request_received",
            "webhook_received",
            "portone_status_fetched",
            "payment_approved",
            "payment_cancelled",
            "payment_partial_cancelled",
            "payment_failed",
            "payment_ignored",
            -> eventType.lowercase()
            else -> "other"
        }

    private fun normalizeAuditResult(result: String): String =
        when (result.lowercase()) {
            "success", "failure", "ignored", "event", "audit_record_failure" -> result.lowercase()
            else -> "other"
        }

    private fun normalizeReason(reason: String): String {
        if (reason.isBlank()) return NONE
        return when (reason.uppercase()) {
            NONE.uppercase(),
            "PAID",
            "FAILED",
            "CANCELLED",
            "PARTIAL_CANCELLED",
            "READY",
            "PAYMENT_ORDER_NOT_FOUND",
            "PAYMENT_ORDER_ALREADY_PROCESSED",
            "PAYMENT_ORDER_FORBIDDEN",
            "PAYMENT_AMOUNT_MISMATCH",
            "PAYMENT_APPROVAL_FAILED",
            "PAYMENT_CANCEL_FAILED",
            "PAYMENT_INVALID_QUANTITY",
            "PAYMENT_QUANTITY_EXCEEDED",
            "PAYMENT_GROUPBUY_NOT_AVAILABLE",
            "PAYMENT_PER_USER_LIMIT_EXCEEDED",
            "PAYMENT_AGREEMENT_REQUIRED",
            "PAYMENT_DUPLICATE_PARTICIPATION",
            "PAYMENT_WEBHOOK_INVALID",
            "PAYMENT_REFUND_NOT_ALLOWED_AFTER_ACHIEVED",
            "GROUPBUY_LOCK_ACQUISITION_FAILED",
            "PARTICIPATION_NOT_FOUND",
            "PARTICIPATION_CANCEL_NOT_ALLOWED",
            "PARTICIPATION_CANCEL_REASON_DETAIL_REQUIRED",
            "PORTONE_UNEXPECTED_STATUS",
            "PENDING_REFUND_TARGET_NOT_FOUND",
            "USER_NOT_FOUND",
            "GROUPBUY_NOT_FOUND",
            "FORBIDDEN",
            -> reason.lowercase()
            else -> "other"
        }
    }

    companion object {
        private const val NONE = "none"
    }
}
