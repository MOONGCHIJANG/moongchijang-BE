package com.moongchijang.domain.payment.infrastructure.portone

import com.moongchijang.domain.payment.application.port.PortOnePaymentPort
import com.moongchijang.domain.payment.application.port.PortOnePaymentResult
import com.moongchijang.global.config.PortOneProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@Component
class PortOnePaymentClient(
    private val portOneProperties: PortOneProperties,
    restClientBuilder: RestClient.Builder,
) : PortOnePaymentPort {
    private val restClient = restClientBuilder.build()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getPayment(paymentId: String): PortOnePaymentResult {
        val startedAtMs = System.currentTimeMillis()
        val response = try {
            restClient.get()
                .uri("${portOneProperties.paymentApiBaseUrl}/payments/{paymentId}", paymentId)
                .header("Authorization", "PortOne ${portOneProperties.apiSecret}")
                .retrieve()
                .body(Map::class.java)
                ?: throw CustomException(ErrorCode.PAYMENT_APPROVAL_FAILED)
        } catch (e: RestClientException) {
            log.warn(
                "[PortOnePaymentClient] 결제 단건 조회 실패: paymentId={}, elapsedMs={}",
                paymentId,
                System.currentTimeMillis() - startedAtMs,
                e,
            )
            throw CustomException(ErrorCode.PAYMENT_APPROVAL_FAILED)
        }
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        if (elapsedMs >= SLOW_REQUEST_WARN_THRESHOLD_MS) {
            log.warn("[PortOnePaymentClient] 결제 단건 조회 지연: paymentId={}, elapsedMs={}", paymentId, elapsedMs)
        }

        return try {
            toPaymentResult(paymentId, response)
        } catch (e: CustomException) {
            throw e
        } catch (e: RuntimeException) {
            throw CustomException(ErrorCode.PAYMENT_APPROVAL_FAILED)
        }
    }

    override fun cancelPayment(paymentId: String, reason: String, cancelAmount: Int?): PortOnePaymentResult {
        val requestBody = mutableMapOf<String, Any>("reason" to reason).apply {
            if (cancelAmount != null) {
                this["amount"] = cancelAmount
            }
        }
        val startedAtMs = System.currentTimeMillis()
        val response = try {
            restClient.post()
                .uri("${portOneProperties.paymentApiBaseUrl}/payments/{paymentId}/cancel", paymentId)
                .header("Authorization", "PortOne ${portOneProperties.apiSecret}")
                .body(requestBody)
                .retrieve()
                .body(Map::class.java)
                ?: throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
        } catch (e: RestClientException) {
            log.warn(
                "[PortOnePaymentClient] 결제 취소 요청 실패: paymentId={}, elapsedMs={}",
                paymentId,
                System.currentTimeMillis() - startedAtMs,
                e,
            )
            throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
        }
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        if (elapsedMs >= SLOW_REQUEST_WARN_THRESHOLD_MS) {
            log.warn("[PortOnePaymentClient] 결제 취소 요청 지연: paymentId={}, elapsedMs={}", paymentId, elapsedMs)
        }

        return try {
            toPaymentResult(paymentId, response)
        } catch (e: CustomException) {
            throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
        } catch (e: RuntimeException) {
            throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
        }
    }

    private fun toPaymentResult(paymentId: String, response: Map<*, *>): PortOnePaymentResult {
        return PortOnePaymentResult(
            paymentId = response["id"] as? String ?: paymentId,
            status = response["status"] as? String ?: "",
            totalAmount = extractTotalAmount(response),
            method = extractPaymentMethod(response),
            paidAt = parseDateTime(response["paidAt"] as? String ?: response["approvedAt"] as? String),
            cancelledAt = parseDateTime(response["cancelledAt"] as? String ?: response["canceledAt"] as? String),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractTotalAmount(response: Map<*, *>): Int {
        val amount = response["amount"] as? Map<String, Any?>
        return (amount?.get("total") as? Number)?.toInt()
            ?: (response["totalAmount"] as? Number)?.toInt()
            ?: throw CustomException(ErrorCode.PAYMENT_APPROVAL_FAILED)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractPaymentMethod(response: Map<*, *>): String? {
        val method = response["method"] as? Map<String, Any?>
        val paymentMethod = response["paymentMethod"] as? Map<String, Any?>
        return method?.keys?.firstOrNull()?.toString()
            ?: paymentMethod?.keys?.firstOrNull()?.toString()
            ?: response["payMethod"] as? String
    }

    private fun parseDateTime(value: String?): LocalDateTime? {
        return value?.let {
            try {
                OffsetDateTime.parse(it).toLocalDateTime()
            } catch (e: DateTimeParseException) {
                LocalDateTime.parse(it)
            }
        }
    }

    companion object {
        private const val SLOW_REQUEST_WARN_THRESHOLD_MS = 1_000L
    }
}
