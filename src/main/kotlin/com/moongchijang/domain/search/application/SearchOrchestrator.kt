package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.search.application.dto.GroupBuyCardDto
import com.moongchijang.domain.search.application.dto.KeywordExtractionResult
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.application.dto.SearchResponse
import com.moongchijang.domain.search.domain.SearchIntent
import com.moongchijang.domain.search.infrastructure.gemini.GeminiKeywordExtractionService
import org.springframework.stereotype.Component

@Component
class SearchOrchestrator(
    private val groupBuyRepository: GroupBuyRepository,
    private val keywordExtractor: GeminiKeywordExtractionService,
    private val aliasDictionary: AliasDictionary,
    private val retrievalPipeline: RetrievalPipeline,
    private val decisionEngine: SearchDecisionEngine
) {
    fun search(query: String): SearchResponse {
        val validRegions = groupBuyRepository.findDistinctRegions(GroupBuyStatus.IN_PROGRESS)
        val validProducts = groupBuyRepository.findDistinctProductNames(GroupBuyStatus.IN_PROGRESS)

        val extraction = extract(query, validRegions, validProducts)
        val product = extraction.product ?: aliasDictionary.resolveProduct(query, validProducts)
        val intent = SearchIntent(
            region = extraction.region,
            product = product,
            searchCase = determineSearchCase(extraction.region, product),
            confidence = confidence(extraction.region, product)
        )

        val candidates = retrievalPipeline.retrieve(query, intent)
        val uiState = decisionEngine.decide(intent, candidates.size)

        return SearchResponse(
            searchCase = intent.searchCase,
            detectedRegion = intent.region,
            detectedProduct = intent.product,
            confidence = intent.confidence,
            uiState = uiState,
            totalCount = candidates.size,
            results = candidates.map { GroupBuyCardDto.from(it.groupBuy) }
        )
    }

    private fun extract(
        query: String,
        validRegions: List<String>,
        validProducts: List<String>
    ): KeywordExtractionResult =
        try {
            keywordExtractor.extract(query, validRegions, validProducts)
        } catch (e: Exception) {
            KeywordExtractionResult(null, null, SearchCase.NONE_DETECTED)
        }

    private fun determineSearchCase(region: String?, product: String?): SearchCase =
        when {
            region != null && product != null -> SearchCase.BOTH_DETECTED
            product != null -> SearchCase.PRODUCT_ONLY
            region != null -> SearchCase.NEIGHBORHOOD_ONLY
            else -> SearchCase.NONE_DETECTED
        }

    private fun confidence(region: String?, product: String?): Double =
        when {
            region != null && product != null -> 0.9
            region != null || product != null -> 0.65
            else -> 0.0
        }
}
