package com.moongchijang.domain.payment.infrastructure.portone

import com.moongchijang.global.config.PortOneProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.LocalDateTime

class PortOnePaymentClientTest {

    @Test
    fun `offset 없는 결제 승인 시각도 파싱한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = PortOnePaymentClient(
            portOneProperties = PortOneProperties(
                storeId = "store-test",
                channelKey = "channel-test",
                apiSecret = "secret-test",
                paymentApiBaseUrl = "https://api.portone.test"
            ),
            restClientBuilder = builder
        )
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
        server.verify()
    }
}
