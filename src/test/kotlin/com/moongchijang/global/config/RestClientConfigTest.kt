package com.moongchijang.global.config

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.RestClientException
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class RestClientConfigTest {

    @Test
    fun `RestClient는 느린 외부 응답에서 read timeout을 적용한다`() {
        val executor = Executors.newSingleThreadExecutor()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/slow") { exchange ->
            Thread.sleep(1_000)
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.close()
        }
        server.executor = executor
        server.start()

        try {
            val config = RestClientConfig(
                HttpClientProperties(
                    connectTimeoutMs = 100,
                    readTimeoutMs = 100,
                )
            )
            val client = config.restClient(config.restClientBuilder())
            val elapsedMs = measureTimeMillis {
                assertThrows<RestClientException> {
                    client.get()
                        .uri("http://127.0.0.1:${server.address.port}/slow")
                        .retrieve()
                        .body(String::class.java)
                }
            }

            assertTrue(elapsedMs < 900, "read timeout should fail before the delayed response completes")
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
    }
}
