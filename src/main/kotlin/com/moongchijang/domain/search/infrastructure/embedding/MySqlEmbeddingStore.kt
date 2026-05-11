package com.moongchijang.domain.search.infrastructure.embedding

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyEmbedding
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyEmbeddingRepository
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingMatch
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.EmbeddingSearchResult
import dev.langchain4j.store.embedding.EmbeddingStore
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import kotlin.math.sqrt

@Component
class MySqlEmbeddingStore(
    private val embeddingRepository: GroupBuyEmbeddingRepository,
    private val objectMapper: ObjectMapper
) : EmbeddingStore<TextSegment> {

    override fun add(embedding: Embedding): String {
        throw UnsupportedOperationException("id 없이 저장하는 방식은 지원하지 않습니다.")
    }

    override fun add(id: String, embedding: Embedding) {
        val groupBuyId = id.toLong()
        val json = objectMapper.writeValueAsString(embedding.vector())
        val existing = embeddingRepository.findByGroupBuyId(groupBuyId)
        if (existing != null) {
            existing.embedding = json
            embeddingRepository.save(existing)
        } else {
            embeddingRepository.save(GroupBuyEmbedding(groupBuyId = groupBuyId, embedding = json))
        }
    }

    override fun add(embedding: Embedding, textSegment: TextSegment): String {
        throw UnsupportedOperationException("id 없이 저장하는 방식은 지원하지 않습니다.")
    }

    override fun addAll(embeddings: List<Embedding>): List<String> {
        throw UnsupportedOperationException("id 없이 저장하는 방식은 지원하지 않습니다.")
    }

    fun delete(id: String) {
        val groupBuyId = id.toLong()
        embeddingRepository.findByGroupBuyId(groupBuyId)?.let {
            embeddingRepository.delete(it)
        }
    }

    override fun search(request: EmbeddingSearchRequest): EmbeddingSearchResult<TextSegment> {
        val queryVector = request.queryEmbedding().vector()
        val maxResults = request.maxResults()
        val minScore = request.minScore()

        val matches = embeddingRepository.findAll()
            .map { stored ->
                val storedVector = readStoredVector(stored.embedding)
                val score = cosineSimilarity(queryVector, storedVector)
                EmbeddingMatch(
                    score.toDouble(),
                    stored.groupBuyId.toString(),
                    Embedding.from(storedVector),
                    TextSegment.from(stored.groupBuyId.toString())
                )
            }
            .filter { it.score() >= minScore }
            .sortedByDescending { it.score() }
            .take(maxResults)

        return EmbeddingSearchResult(matches)
    }

    private fun readStoredVector(rawEmbedding: String): FloatArray {
        val trimmed = rawEmbedding.trim()

        return try {
            objectMapper.readValue(trimmed, FloatArray::class.java)
        } catch (_: Exception) {
            val unwrapped = objectMapper.readValue(trimmed, String::class.java).trim()
            objectMapper.readValue(unwrapped, FloatArray::class.java)
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
    }
}
