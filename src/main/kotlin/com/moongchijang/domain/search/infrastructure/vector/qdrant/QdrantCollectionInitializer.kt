package com.moongchijang.domain.search.infrastructure.vector.qdrant

import com.fasterxml.jackson.annotation.JsonProperty
import com.moongchijang.global.config.QdrantProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

@Component
@ConditionalOnProperty(prefix = "qdrant", name = ["enabled"], havingValue = "true")
class QdrantCollectionInitializer(
    restClientBuilder: RestClient.Builder,
    private val properties: QdrantProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient: RestClient =
        run {
            val builder = restClientBuilder
                .baseUrl(properties.url)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(
                    SimpleClientHttpRequestFactory().apply {
                        setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds))
                        setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds))
                    }
                )

            properties.apiKey?.takeIf { it.isNotBlank() }?.let {
                builder.defaultHeader("api-key", it)
            }

            builder.build()
        }

    @EventListener(ApplicationReadyEvent::class)
    fun initialize() {
        if (!properties.initializeCollection) {
            log.info("Qdrant collection bootstrap skipped: collection={}", properties.collectionName)
            return
        }

        val request = CreateCollectionRequest(
            vectors = VectorParams(
                size = properties.vectorSize,
                distance = properties.distance
            )
        )

        try {
            restClient.put()
                .uri("/collections/{collectionName}", properties.collectionName)
                .body(request)
                .retrieve()
                .toBodilessEntity()
            log.info("Qdrant collection bootstrap requested: collection={}", properties.collectionName)
        } catch (e: Exception) {
            log.warn("Qdrant collection bootstrap failed: collection={}, error={}", properties.collectionName, e.message)
        }
    }

    private data class CreateCollectionRequest(
        val vectors: VectorParams
    )

    private data class VectorParams(
        val size: Int,
        val distance: String,
        @JsonProperty("on_disk")
        val onDisk: Boolean = false
    )
}
