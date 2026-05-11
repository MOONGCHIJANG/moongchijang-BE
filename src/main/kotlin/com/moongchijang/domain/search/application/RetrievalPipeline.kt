package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.search.application.port.VectorSearchPort
import com.moongchijang.domain.search.domain.SearchCandidate
import com.moongchijang.domain.search.domain.SearchIntent
import com.moongchijang.global.config.SearchProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RetrievalPipeline(
    private val groupBuyRepository: GroupBuyRepository,
    private val vectorSearchPort: VectorSearchPort,
    private val aliasDictionary: AliasDictionary,
    private val vectorCandidatePromotionGuard: VectorCandidatePromotionGuard,
    private val reranker: SearchReranker,
    private val searchObservabilityLogger: SearchObservabilityLogger,
    private val searchProperties: SearchProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun retrieve(query: String, intent: SearchIntent): List<SearchCandidate> {
        val now = LocalDateTime.now()
        val exactMatches = groupBuyRepository.searchByIntent(
            region = intent.region,
            product = intent.product,
            now = now,
            status = GroupBuyStatus.IN_PROGRESS
        )

        val vectorCandidates = searchVectorCandidates(query)
        val vectorMatches = groupBuyRepository.findAllById(vectorCandidates.map { it.groupBuyId })
            .filter { it.status == GroupBuyStatus.IN_PROGRESS && it.deadline.isAfter(now) }
            .filter { intent.region == null || it.store.region.label == intent.region }
        val promotableVectorResult = vectorCandidatePromotionGuard.filterPromotableCandidates(
            intent = intent,
            vectorMatches = vectorMatches,
            vectorCandidates = vectorCandidates
        )

        val finalResults = reranker.merge(
            exactMatches = exactMatches,
            vectorMatches = promotableVectorResult.vectorMatches,
            vectorCandidates = promotableVectorResult.vectorCandidates,
            aliasMatched = aliasDictionary.isAliasMatch(query, intent.product)
        )

        searchObservabilityLogger.logRetrieval(
            query = query,
            intent = intent,
            mysqlResultCount = exactMatches.size,
            vectorCandidateCount = vectorCandidates.size,
            promotionResult = promotableVectorResult,
            finalResults = finalResults
        )

        return finalResults
    }

    private fun searchVectorCandidates(query: String) =
        try {
            vectorSearchPort.search(
                query = query,
                topK = searchProperties.retrieval.vectorCandidateLimit,
                minScore = searchProperties.retrieval.vectorMinScore
            )
        } catch (e: Exception) {
            log.warn(
                "Vector search failed, fallbackProvider={} error={}",
                searchProperties.retrieval.fallbackProvider,
                e.message
            )
            emptyList()
        }
}
