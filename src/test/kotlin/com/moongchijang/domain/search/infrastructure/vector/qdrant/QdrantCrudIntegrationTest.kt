package com.moongchijang.domain.search.infrastructure.vector.qdrant

import com.moongchijang.domain.search.application.port.VectorIndexDocument
import com.moongchijang.domain.search.infrastructure.demo.LocalDemoEmbeddingModel
import com.moongchijang.global.config.QdrantProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.time.LocalDateTime

/**
 * Qdrant CRUD 통합 테스트.
 *
 * 실행 조건:
 *  - 환경 변수 QDRANT_URL 설정 (예: http://localhost:6333)
 *  - 선택: QDRANT_API_KEY
 *
 * 로컬에서 docker compose로 qdrant 컨테이너를 띄운 뒤 실행한다.
 * 테스트마다 고유한 collection을 생성/삭제하므로 운영 데이터에 영향 없다.
 */
@EnabledIfEnvironmentVariable(named = "QDRANT_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QdrantCrudIntegrationTest {

    private val qdrantUrl = System.getenv("QDRANT_URL")
    private val qdrantApiKey: String? = System.getenv("QDRANT_API_KEY")
    private val collectionName = "test_groupbuys_${System.currentTimeMillis()}"

    private val embeddingModel = LocalDemoEmbeddingModel(vectorSize = 768)
    private val properties = QdrantProperties(
        enabled = true,
        url = qdrantUrl,
        apiKey = qdrantApiKey,
        collectionName = collectionName,
        timeoutSeconds = 5
    )
    private val rawClient: RestClient = run {
        var builder = RestClient.builder()
            .baseUrl(qdrantUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        qdrantApiKey?.takeIf { it.isNotBlank() }?.let {
            builder = builder.defaultHeader("api-key", it)
        }
        builder.build()
    }

    private val adapter = QdrantVectorSearchAdapter(
        restClientBuilder = RestClient.builder(),
        embeddingModel = embeddingModel,
        properties = properties
    )

    @BeforeAll
    fun createCollection() {
        rawClient.put()
            .uri("/collections/{name}", collectionName)
            .body(mapOf("vectors" to mapOf("size" to 768, "distance" to "Cosine")))
            .retrieve()
            .toBodilessEntity()
    }

    @AfterAll
    fun dropCollection() {
        rawClient.delete()
            .uri("/collections/{name}", collectionName)
            .retrieve()
            .toBodilessEntity()
    }

    @Test
    fun `upsert 후 search로 동일 그룹바이를 검색하면 후보로 반환된다`() {
        val doc = VectorIndexDocument(
            groupBuyId = 9001L,
            vectorText = "성수 두쫀쿠",
            vector = embeddingModel.embed("성수 두쫀쿠").content().vector(),
            region = "서울",
            storeName = "테스트 베이커리",
            productName = "두쫀쿠",
            status = "IN_PROGRESS",
            deadline = LocalDateTime.now().plusDays(3),
            embeddingVersion = "test-v1"
        )
        assertThat(adapter.upsert(doc)).isTrue

        val results = adapter.search(query = "성수 두쫀쿠", topK = 5, minScore = 0.0)

        assertThat(results.map { it.groupBuyId }).contains(9001L)
    }

    @Test
    fun `delete 후 search 시 해당 그룹바이가 더 이상 반환되지 않는다`() {
        val doc = VectorIndexDocument(
            groupBuyId = 9002L,
            vectorText = "홍대 소금빵",
            vector = embeddingModel.embed("홍대 소금빵").content().vector(),
            region = "서울",
            storeName = "홍대 빵집",
            productName = "소금빵",
            status = "IN_PROGRESS",
            deadline = LocalDateTime.now().plusDays(3),
            embeddingVersion = "test-v1"
        )
        adapter.upsert(doc)

        assertThat(adapter.delete(9002L)).isTrue

        val results = adapter.search(query = "홍대 소금빵", topK = 5, minScore = 0.0)
        assertThat(results.map { it.groupBuyId }).doesNotContain(9002L)
    }
}
