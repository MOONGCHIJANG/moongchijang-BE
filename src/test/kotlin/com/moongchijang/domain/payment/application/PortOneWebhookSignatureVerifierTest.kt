package com.moongchijang.domain.payment.application

import com.moongchijang.global.config.PortOneProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PortOneWebhookSignatureVerifierTest {

    private val clock = Clock.fixed(Instant.ofEpochSecond(1_780_000_000), ZoneId.of("Asia/Seoul"))
    private val rawPayload = """{"type":"Transaction.Paid","paymentId":"MCJ-10-test"}"""

    @Test
    fun `signature 검증이 비활성화되어 있으면 검증을 건너뛴다`() {
        val verifier = verifier(secret = null, enabled = false)

        assertThatCode {
            verifier.verify(HttpHeaders(), rawPayload)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `signature 검증이 활성화되어 있는데 webhook secret이 없으면 웹훅 invalid 예외를 던진다`() {
        val verifier = verifier(secret = null)

        assertThatThrownBy {
            verifier.verify(HttpHeaders(), rawPayload)
        }.isInstanceOf(CustomException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PAYMENT_WEBHOOK_INVALID)
    }

    @Test
    fun `정상 signature면 검증에 성공한다`() {
        val secret = "test-webhook-secret"
        val timestamp = clock.instant().epochSecond
        val webhookId = "evt-1"
        val headers = HttpHeaders().apply {
            set("webhook-id", webhookId)
            set("webhook-timestamp", timestamp.toString())
            set("webhook-signature", "v1,${signature(secret, webhookId, timestamp, rawPayload)}")
        }

        assertThatCode {
            verifier(secret).verify(headers, rawPayload)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `여러 signature 중 하나가 일치하면 검증에 성공한다`() {
        val secret = "test-webhook-secret"
        val timestamp = clock.instant().epochSecond
        val webhookId = "evt-1"
        val validSignature = signature(secret, webhookId, timestamp, rawPayload)
        val headers = HttpHeaders().apply {
            set("webhook-id", webhookId)
            set("webhook-timestamp", timestamp.toString())
            set("webhook-signature", "v1,invalid v1,$validSignature")
        }

        assertThatCode {
            verifier(secret).verify(headers, rawPayload)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `signature가 일치하지 않으면 웹훅 invalid 예외를 던진다`() {
        val headers = HttpHeaders().apply {
            set("webhook-id", "evt-1")
            set("webhook-timestamp", clock.instant().epochSecond.toString())
            set("webhook-signature", "v1,invalid")
        }

        assertThatThrownBy {
            verifier("test-webhook-secret").verify(headers, rawPayload)
        }.isInstanceOf(CustomException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PAYMENT_WEBHOOK_INVALID)
    }

    @Test
    fun `timestamp 허용 범위를 벗어나면 웹훅 invalid 예외를 던진다`() {
        val secret = "test-webhook-secret"
        val timestamp = clock.instant().epochSecond - 301
        val webhookId = "evt-1"
        val headers = HttpHeaders().apply {
            set("webhook-id", webhookId)
            set("webhook-timestamp", timestamp.toString())
            set("webhook-signature", "v1,${signature(secret, webhookId, timestamp, rawPayload)}")
        }

        assertThatThrownBy {
            verifier(secret).verify(headers, rawPayload)
        }.isInstanceOf(CustomException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PAYMENT_WEBHOOK_INVALID)
    }

    @Test
    fun `whsec prefix가 있는 base64 secret도 검증한다`() {
        val rawSecret = "test-webhook-secret"
        val secret = "whsec_${Base64.getEncoder().encodeToString(rawSecret.toByteArray())}"
        val timestamp = clock.instant().epochSecond
        val webhookId = "evt-1"
        val headers = HttpHeaders().apply {
            set("webhook-id", webhookId)
            set("webhook-timestamp", timestamp.toString())
            set("webhook-signature", "v1,${signature(rawSecret, webhookId, timestamp, rawPayload)}")
        }

        assertThatCode {
            verifier(secret).verify(headers, rawPayload)
        }.doesNotThrowAnyException()
    }

    private fun verifier(secret: String?, enabled: Boolean = true): PortOneWebhookSignatureVerifier =
        PortOneWebhookSignatureVerifier(
            portOneProperties = PortOneProperties(
                storeId = "store-test",
                channelKey = "channel-test",
                apiSecret = "api-secret",
                webhookSignatureVerificationEnabled = enabled,
                webhookSecret = secret,
            ),
            clock = clock,
        )

    private fun signature(secret: String, webhookId: String, timestamp: Long, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal("$webhookId.$timestamp.$payload".toByteArray()))
    }
}
