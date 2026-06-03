package com.moongchijang.domain.payment.application

import com.moongchijang.global.config.PortOneProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class PortOneWebhookSignatureVerifier(
    private val portOneProperties: PortOneProperties,
    private val clock: Clock,
) {

    fun verify(headers: HttpHeaders, rawPayload: String) {
        val secret = portOneProperties.webhookSecret?.takeIf { it.isNotBlank() } ?: return
        val webhookId = headers.getFirst(WEBHOOK_ID_HEADER)
            ?: throw CustomException(ErrorCode.PAYMENT_WEBHOOK_INVALID)
        val timestamp = headers.getFirst(WEBHOOK_TIMESTAMP_HEADER)?.toLongOrNull()
            ?: throw CustomException(ErrorCode.PAYMENT_WEBHOOK_INVALID)
        val signatureHeader = headers.getFirst(WEBHOOK_SIGNATURE_HEADER)
            ?: throw CustomException(ErrorCode.PAYMENT_WEBHOOK_INVALID)

        val now = clock.instant().epochSecond
        if (kotlin.math.abs(now - timestamp) > portOneProperties.webhookTimestampToleranceSeconds) {
            throw CustomException(ErrorCode.PAYMENT_WEBHOOK_INVALID)
        }

        val signedPayload = "$webhookId.$timestamp.$rawPayload"
        val expectedSignature = hmacSha256Base64(secret, signedPayload)
        val matched = extractV1Signatures(signatureHeader)
            .any { constantTimeEquals(expectedSignature, it) }

        if (!matched) {
            throw CustomException(ErrorCode.PAYMENT_WEBHOOK_INVALID)
        }
    }

    private fun extractV1Signatures(signatureHeader: String): Sequence<String> =
        signatureHeader
            .split(" ")
            .asSequence()
            .flatMap { entry ->
                entry.split(",")
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .windowed(size = 2, step = 2, partialWindows = false)
                    .asSequence()
                    .filter { it[0] == SIGNATURE_VERSION }
                    .map { it[1] }
            }

    private fun hmacSha256Base64(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKeyBytes(secret), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(Charsets.UTF_8)))
    }

    private fun secretKeyBytes(secret: String): ByteArray {
        if (!secret.startsWith(WEBHOOK_SECRET_PREFIX)) {
            return secret.toByteArray(Charsets.UTF_8)
        }
        val normalized = secret.removePrefix(WEBHOOK_SECRET_PREFIX)
        return try {
            Base64.getDecoder().decode(normalized)
        } catch (e: IllegalArgumentException) {
            secret.toByteArray(Charsets.UTF_8)
        }
    }

    private fun constantTimeEquals(expected: String, actual: String): Boolean =
        MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            actual.toByteArray(Charsets.UTF_8)
        )

    companion object {
        private const val SIGNATURE_VERSION = "v1"
        private const val WEBHOOK_SECRET_PREFIX = "whsec_"
        private const val WEBHOOK_ID_HEADER = "webhook-id"
        private const val WEBHOOK_TIMESTAMP_HEADER = "webhook-timestamp"
        private const val WEBHOOK_SIGNATURE_HEADER = "webhook-signature"
    }
}
