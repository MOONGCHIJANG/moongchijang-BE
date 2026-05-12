package com.moongchijang.domain.search.application.port

data class VectorSearchCandidate(
    val groupBuyId: Long,
    val score: Double
)

interface VectorSearchPort {
    fun search(query: String, topK: Int, minScore: Double): List<VectorSearchCandidate>
}
