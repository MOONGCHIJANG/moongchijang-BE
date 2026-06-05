package com.moongchijang.domain.payment.infrastructure.portone

import com.moongchijang.domain.payment.application.port.PortOnePaymentPort
import com.moongchijang.domain.payment.application.port.PortOnePaymentResult
import com.moongchijang.global.config.PortOneProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
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
        val partial = cancelAmount != null
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
        } catch (e: HttpClientErrorException) {
            val elapsedMs = System.currentTimeMillis() - startedAtMs
            if (isAlreadyCancelledResponse(e)) {
                log.info(
                    "[PortOnePaymentClient] PortOne 이미 취소된 결제 멱등 처리: paymentId={}, elapsedMs={}",
                    paymentId,
                    elapsedMs,
                )
                return alreadyCancelledResult(paymentId, partial, cancelAmount)
            }
            log.warn(
                "[PortOnePaymentClient] 결제 취소 요청 실패: paymentId={}, elapsedMs={}, body={}",
                paymentId,
                elapsedMs,
                e.responseBodyAsString,
                e,
            )
            throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
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
            toCancellationResult(paymentId, response, partial)
        } catch (e: CustomException) {
            log.warn(
                "[PortOnePaymentClient] 결제 취소 응답 파싱 실패: paymentId={}, response={}",
                paymentId,
                response,
            )
            throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
        } catch (e: RuntimeException) {
            log.warn(
                "[PortOnePaymentClient] 결제 취소 응답 파싱 실패: paymentId={}, response={}",
                paymentId,
                response,
                e,
            )
            throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
        }
    }

    private fun isAlreadyCancelledResponse(e: HttpClientErrorException): Boolean {
        if (e.statusCode != HttpStatus.CONFLICT) return false
        return e.responseBodyAsString.contains(PORTONE_ERROR_PAYMENT_ALREADY_CANCELLED)
    }

    private fun alreadyCancelledResult(
        paymentId: String,
        partial: Boolean,
        cancelAmount: Int?,
    ): PortOnePaymentResult {
        return PortOnePaymentResult(
            paymentId = paymentId,
            status = if (partial) PORTONE_STATUS_PARTIAL_CANCELLED else PORTONE_STATUS_CANCELLED,
            totalAmount = cancelAmount ?: 0,
            method = null,
            paidAt = null,
            cancelledAt = null,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun toCancellationResult(
        paymentId: String,
        response: Map<*, *>,
        partial: Boolean,
    ): PortOnePaymentResult {
        val cancellation = response["cancellation"] as? Map<String, Any?>
            ?: throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
        val rawStatus = cancellation["status"] as? String ?: ""
        val mappedStatus = when (rawStatus) {
            PORTONE_CANCELLATION_STATUS_SUCCEEDED ->
                if (partial) PORTONE_STATUS_PARTIAL_CANCELLED else PORTONE_STATUS_CANCELLED
            else -> rawStatus
        }
        val totalAmount = (cancellation["totalAmount"] as? Number)?.toInt() ?: 0
        val cancelledAt = parseDateTime(cancellation["cancelledAt"] as? String)
        return PortOnePaymentResult(
            paymentId = paymentId,
            status = mappedStatus,
            totalAmount = totalAmount,
            method = null,
            paidAt = null,
            cancelledAt = cancelledAt,
        )
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
        private const val PORTONE_CANCELLATION_STATUS_SUCCEEDED = "SUCCEEDED"
        private const val PORTONE_STATUS_CANCELLED = "CANCELLED"
        private const val PORTONE_STATUS_PARTIAL_CANCELLED = "PARTIAL_CANCELLED"
        private const val PORTONE_ERROR_PAYMENT_ALREADY_CANCELLED = "PAYMENT_ALREADY_CANCELLED"
    }
}
