package com.moongchijang.domain.payment.infrastructure.portone

import com.moongchijang.domain.payment.application.PaymentMetricsRecorder
import com.moongchijang.global.config.PortOneProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.LocalDateTime

class PortOnePaymentClientTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val paymentMetricsRecorder = PaymentMetricsRecorder(meterRegistry)

    @Test
    fun `offset 없는 결제 승인 시각도 파싱한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        val paymentId = "MCJ-901003-da4c41e875f7431dbfa3"

        server.expect(once(), requestTo("https://api.portone.test/payments/$paymentId"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "PortOne secret-test"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "id": "$paymentId",
                      "status": "PAID",
                      "amount": { "total": 1000 },
                      "method": { "card": {} },
                      "paidAt": "2026-05-19T06:20:00"
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val result = client.getPayment(paymentId)

        assertEquals(paymentId, result.paymentId)
        assertEquals("PAID", result.status)
        assertEquals(1000, result.totalAmount)
        assertEquals("card", result.method)
        assertEquals(LocalDateTime.of(2026, 5, 19, 6, 20), result.paidAt)
        assertEquals(
            1.0,
            meterRegistry.get("mcj_portone_api_requests_total")
                .tags("operation", "get_payment", "result", "success", "status", "paid")
                .counter()
                .count(),
        )
        server.verify()
    }

    @Test
    fun `포트원 결제 취소 응답의 cancellation 래퍼를 파싱한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        val paymentId = "portone-payment-id"

        server.expect(once(), requestTo("https://api.portone.test/payments/$paymentId/cancel"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "PortOne secret-test"))
            .andExpect(content().json("""{"reason":"OTHER: 시간이 맞지 않습니다"}"""))
            .andRespond(
                withSuccess(
                    """
                    {
                      "cancellation": {
                        "id": "cancellation-id-1",
                        "status": "SUCCEEDED",
                        "totalAmount": 12000,
                        "cancelledAt": "2026-05-19T06:25:00"
                      }
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val result = client.cancelPayment(paymentId, "OTHER: 시간이 맞지 않습니다")

        assertEquals(paymentId, result.paymentId)
        assertEquals("CANCELLED", result.status)
        assertEquals(12000, result.totalAmount)
        assertEquals(LocalDateTime.of(2026, 5, 19, 6, 25), result.cancelledAt)
        server.verify()
    }

    @Test
    fun `부분 취소 요청 시 cancelAmount를 본문에 담고 부분취소 상태로 매핑한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        val paymentId = "portone-payment-id"

        server.expect(once(), requestTo("https://api.portone.test/payments/$paymentId/cancel"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""{"reason":"환불 대기 처리","amount":5000}"""))
            .andRespond(
                withSuccess(
                    """
                    {
                      "cancellation": {
                        "id": "cancellation-id-2",
                        "status": "SUCCEEDED",
                        "totalAmount": 5000,
                        "cancelledAt": "2026-05-20T09:00:00"
                      }
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val result = client.cancelPayment(paymentId, "환불 대기 처리", cancelAmount = 5000)

        assertEquals("PARTIAL_CANCELLED", result.status)
        assertEquals(5000, result.totalAmount)
        assertEquals(LocalDateTime.of(2026, 5, 20, 9, 0), result.cancelledAt)
        server.verify()
    }

    @Test
    fun `이미 취소된 결제는 PAYMENT_ALREADY_CANCELLED를 받아도 취소 완료로 간주한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        val paymentId = "portone-payment-id"

        server.expect(once(), requestTo("https://api.portone.test/payments/$paymentId/cancel"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"type":"PAYMENT_ALREADY_CANCELLED","message":"payment already cancelled"}""")
            )

        val result = client.cancelPayment(paymentId, "OTHER: 시간이 맞지 않습니다")

        assertEquals(paymentId, result.paymentId)
        assertEquals("CANCELLED", result.status)
        server.verify()
    }

    private fun client(builder: RestClient.Builder) =
        PortOnePaymentClient(
            portOneProperties = PortOneProperties(
                storeId = "store-test",
                channelKey = "channel-test",
                apiSecret = "secret-test",
                paymentApiBaseUrl = "https://api.portone.test"
            ),
            paymentMetricsRecorder = paymentMetricsRecorder,
            restClientBuilder = builder
        )
}
