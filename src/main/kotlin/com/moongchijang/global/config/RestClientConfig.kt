package com.moongchijang.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RestClientConfig(
    private val httpClientProperties: HttpClientProperties,
) {

    @Bean
    fun restClientBuilder(): RestClient.Builder =
        RestClient.builder()
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(Duration.ofMillis(httpClientProperties.connectTimeoutMs.coerceAtLeast(1)))
                    setReadTimeout(Duration.ofMillis(httpClientProperties.readTimeoutMs.coerceAtLeast(1)))
                }
            )

    @Bean
    fun restClient(builder: RestClient.Builder): RestClient = builder.build()
}
