package com.moongchijang.domain.payment.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.moongchijang.domain.payment.application.PaymentMetricsRecorder
import com.moongchijang.domain.payment.application.PaymentService
import com.moongchijang.domain.payment.application.PortOneWebhookSignatureVerifier
import com.moongchijang.domain.payment.application.dto.PortOneWebhookRequest
import com.moongchijang.global.config.PortOneProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.http.HttpHeaders

class PaymentControllerTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val paymentMetricsRecorder = PaymentMetricsRecorder(meterRegistry)

    @Test
    fun `웹훅 signature 검증 실패는 invalid webhook metric을 기록한다`() {
        val controller = controller(
            portOneWebhookSignatureVerifier = verifier(signatureVerificationEnabled = true),
        )

        val exception = assertThrows<CustomException> {
            controller.handlePortOneWebhook(rawPayload = """{"paymentId":"MCJ-10-test"}""", headers = HttpHeaders())
        }

        assertEquals(ErrorCode.PAYMENT_WEBHOOK_INVALID, exception.errorCode)
        assertCounter(
            "mcj_payment_webhook_processed_total",
            1.0,
            "event_type", "unknown",
            "result", "failure",
            "reason", "payment_webhook_invalid",
        )
    }

    @Test
    fun `웹훅 JSON 파싱 실패는 invalid webhook metric을 기록한다`() {
        val controller = controller(
            portOneWebhookSignatureVerifier = verifier(signatureVerificationEnabled = false),
        )

        val exception = assertThrows<CustomException> {
            controller.handlePortOneWebhook(rawPayload = "{not-json", headers = HttpHeaders())
        }

        assertEquals(ErrorCode.PAYMENT_WEBHOOK_INVALID, exception.errorCode)
        assertCounter(
            "mcj_payment_webhook_processed_total",
            1.0,
            "event_type", "unknown",
            "result", "failure",
            "reason", "payment_webhook_invalid",
        )
    }

    @Test
    fun `PortOne V2 웹훅 payload는 data 필드에서 결제 식별자를 파싱한다`() {
        val paymentService = mock(PaymentService::class.java)
        val controller = controller(
            paymentService = paymentService,
            portOneWebhookSignatureVerifier = verifier(signatureVerificationEnabled = false),
        )
        val rawPayload = """
            {
              "type": "Transaction.Paid",
              "timestamp": "2026-06-26T00:00:00.000Z",
              "data": {
                "storeId": "store-test",
                "paymentId": "MCJ-10-test",
                "transactionId": "tx-test"
              }
            }
        """.trimIndent()

        controller.handlePortOneWebhook(rawPayload = rawPayload, headers = HttpHeaders())

        verify(paymentService).handlePortOneWebhook(
            PortOneWebhookRequest(type = "Transaction.Paid", storeId = "store-test", paymentId = "MCJ-10-test"),
            rawPayload,
        )
    }

    private fun controller(
        portOneWebhookSignatureVerifier: PortOneWebhookSignatureVerifier,
        paymentService: PaymentService = mock(PaymentService::class.java),
    ): PaymentController =
        PaymentController(
            paymentService = paymentService,
            portOneWebhookSignatureVerifier = portOneWebhookSignatureVerifier,
            objectMapper = ObjectMapper(),
            paymentMetricsRecorder = paymentMetricsRecorder,
        )

    private fun verifier(signatureVerificationEnabled: Boolean): PortOneWebhookSignatureVerifier =
        PortOneWebhookSignatureVerifier(
            portOneProperties = PortOneProperties(
                storeId = "store-test",
                channelKey = "channel-test",
                apiSecret = "api-secret-test",
                webhookSignatureVerificationEnabled = signatureVerificationEnabled,
                webhookSecret = "webhook-secret-test",
            ),
            clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC),
        )

    private fun assertCounter(name: String, expected: Double, vararg tags: String) {
        val counter = meterRegistry.find(name).tags(*tags).counter()

        assertEquals(expected, counter?.count())
    }
}
