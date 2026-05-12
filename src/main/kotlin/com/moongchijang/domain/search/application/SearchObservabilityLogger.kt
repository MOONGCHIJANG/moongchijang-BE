package com.moongchijang.domain.search.application

import com.moongchijang.domain.search.domain.SearchCandidate
import com.moongchijang.domain.search.domain.SearchIntent
import com.moongchijang.global.config.SearchProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class SearchObservabilityLogger(
    private val searchProperties: SearchProperties = SearchProperties()
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun logRetrieval(
        query: String,
        intent: SearchIntent,
        mysqlResultCount: Int,
        vectorCandidateCount: Int,
        promotionResult: VectorPromotionResult,
        finalResults: List<SearchCandidate>
    ) {
        if (!searchProperties.observability.enabled) return
        if (!log.isInfoEnabled) return

        log.info(
            "search_retrieval " +
                "queryHash={} queryLength={} searchCase={} hasRegion={} hasProduct={} confidenceBucket={} " +
                "mysqlResultCount={} qdrantCandidateCount={} vectorPromotedCount={} guardRejectedCount={} " +
                "finalResultCount={} vectorOnlyResultCount={} topCandidateScore={} providerSources={} guardRejectionReasons={}",
            queryHash(query),
            query.length,
            intent.searchCase,
            intent.region != null,
            intent.product != null,
            confidenceBucket(intent.confidence),
            mysqlResultCount,
            vectorCandidateCount,
            promotionResult.vectorCandidates.size,
            promotionResult.rejectedCount,
            finalResults.size,
            vectorOnlyResultCount(finalResults),
            topCandidateScore(promotionResult),
            providerSources(finalResults),
            promotionResult.rejectionReasonCounts.mapKeys { it.key.name }
        )
    }

    private fun queryHash(query: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(query.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun confidenceBucket(confidence: Double): String =
        when {
            confidence >= 0.9 -> "HIGH"
            confidence >= 0.65 -> "MEDIUM"
            confidence > 0.0 -> "LOW"
            else -> "NONE"
        }

    private fun topCandidateScore(promotionResult: VectorPromotionResult): Double? =
        promotionResult.decisions.maxOfOrNull { it.score }

    private fun providerSources(finalResults: List<SearchCandidate>): Map<String, Int> =
        finalResults
            .flatMap { it.matchedBy }
            .groupingBy { it }
            .eachCount()

    private fun vectorOnlyResultCount(finalResults: List<SearchCandidate>): Int =
        finalResults.count { it.matchedBy == setOf("VECTOR") }
}
