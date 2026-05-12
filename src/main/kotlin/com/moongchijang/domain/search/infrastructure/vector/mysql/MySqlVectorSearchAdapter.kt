package com.moongchijang.domain.search.infrastructure.vector.mysql

import com.moongchijang.domain.search.application.port.VectorIndexDocument
import com.moongchijang.domain.search.application.port.VectorIndexPort
import com.moongchijang.domain.search.application.port.VectorSearchCandidate
import com.moongchijang.domain.search.application.port.VectorSearchPort
import com.moongchijang.domain.search.infrastructure.embedding.MySqlEmbeddingStore
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "qdrant", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class MySqlVectorSearchAdapter(
    private val embeddingModel: EmbeddingModel,
    private val embeddingStore: MySqlEmbeddingStore
) : VectorSearchPort, VectorIndexPort {
    override fun search(query: String, topK: Int, minScore: Double): List<VectorSearchCandidate> {
        val queryEmbedding = embeddingModel.embed(query).content()
        val searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(topK)
            .minScore(minScore)
            .build()

        return embeddingStore.search(searchRequest).matches()
            .mapNotNull { match ->
                match.embedded().text().toLongOrNull()?.let { groupBuyId ->
                    VectorSearchCandidate(
                        groupBuyId = groupBuyId,
                        score = match.score()
                    )
                }
            }
    }

    override fun upsert(document: VectorIndexDocument): Boolean {
        embeddingStore.add(document.groupBuyId.toString(), Embedding.from(document.vector))
        return true
    }

    override fun delete(groupBuyId: Long): Boolean {
        embeddingStore.delete(groupBuyId.toString())
        return true
    }
}
