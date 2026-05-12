package com.moongchijang.domain.search.infrastructure.vector.qdrant

import com.fasterxml.jackson.annotation.JsonProperty
import com.moongchijang.domain.search.application.port.VectorIndexDocument
import com.moongchijang.domain.search.application.port.VectorIndexPort
import com.moongchijang.domain.search.application.port.VectorSearchCandidate
import com.moongchijang.domain.search.application.port.VectorSearchPort
import com.moongchijang.global.config.QdrantProperties
import dev.langchain4j.model.embedding.EmbeddingModel
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

@Component
@ConditionalOnProperty(prefix = "qdrant", name = ["enabled"], havingValue = "true")
class QdrantVectorSearchAdapter(
    restClientBuilder: RestClient.Builder,
    private val embeddingModel: EmbeddingModel,
    private val properties: QdrantProperties
) : VectorSearchPort, VectorIndexPort {
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

    override fun search(query: String, topK: Int, minScore: Double): List<VectorSearchCandidate> {
        return try {
            val vector = embeddingModel.embed(query).content().vector().map { it.toDouble() }
            val request = QdrantQueryRequest(
                query = vector,
                limit = topK,
                scoreThreshold = minScore,
                withPayload = listOf("groupBuyId", "status", "deadline")
            )

            val response = restClient.post()
                .uri("/collections/{collectionName}/points/query", properties.collectionName)
                .body(request)
                .retrieve()
                .body(QdrantQueryResponse::class.java)

            response?.result?.points
                ?.mapNotNull { point ->
                    val groupBuyId = point.payload?.groupBuyId ?: point.id.toLongOrNull()
                    groupBuyId?.let {
                        VectorSearchCandidate(
                            groupBuyId = it,
                            score = point.score
                        )
                    }
                }
                ?: emptyList()
        } catch (e: Exception) {
            log.warn("Qdrant 검색 실패, vector 후보 없이 fallback: error={}", e.message)
            emptyList()
        }
    }

    override fun upsert(document: VectorIndexDocument): Boolean {
        val request = QdrantUpsertRequest(
            points = listOf(
                QdrantPointUpsert(
                    id = document.groupBuyId,
                    vector = document.vector.map { it.toDouble() },
                    payload = QdrantUpsertPayload(
                        groupBuyId = document.groupBuyId,
                        vectorText = document.vectorText,
                        region = document.region,
                        storeName = document.storeName,
                        productName = document.productName,
                        status = document.status,
                        deadline = document.deadline.toString(),
                        embeddingVersion = document.embeddingVersion
                    )
                )
            )
        )

        return try {
            restClient.put()
                .uri("/collections/{collectionName}/points?wait=true", properties.collectionName)
                .body(request)
                .retrieve()
                .toBodilessEntity()
            true
        } catch (e: Exception) {
            log.warn("Qdrant upsert 실패: groupBuyId={}, error={}", document.groupBuyId, e.message)
            false
        }
    }

    override fun delete(groupBuyId: Long): Boolean {
        val request = QdrantDeleteRequest(points = listOf(groupBuyId))

        return try {
            restClient.post()
                .uri("/collections/{collectionName}/points/delete?wait=true", properties.collectionName)
                .body(request)
                .retrieve()
                .toBodilessEntity()
            true
        } catch (e: Exception) {
            log.warn("Qdrant delete 실패: groupBuyId={}, error={}", groupBuyId, e.message)
            false
        }
    }

    private data class QdrantQueryRequest(
        val query: List<Double>,
        val limit: Int,
        @JsonProperty("with_payload")
        val withPayload: List<String>,
        @JsonProperty("score_threshold")
        val scoreThreshold: Double
    )

    private data class QdrantQueryResponse(
        val result: QdrantQueryResult? = null
    )

    private data class QdrantQueryResult(
        val points: List<QdrantPoint> = emptyList()
    )

    private data class QdrantPoint(
        val id: String,
        val score: Double,
        val payload: QdrantPayload? = null
    )

    private data class QdrantPayload(
        val groupBuyId: Long? = null,
        val status: String? = null,
        val deadline: String? = null
    )

    private data class QdrantUpsertRequest(
        val points: List<QdrantPointUpsert>
    )

    private data class QdrantPointUpsert(
        val id: Long,
        val vector: List<Double>,
        val payload: QdrantUpsertPayload
    )

    private data class QdrantUpsertPayload(
        val groupBuyId: Long,
        @JsonProperty("vector_text")
        val vectorText: String,
        val region: String,
        val storeName: String,
        val productName: String,
        val status: String,
        val deadline: String,
        val embeddingVersion: String
    )

    private data class QdrantDeleteRequest(
        val points: List<Long>
    )
}
