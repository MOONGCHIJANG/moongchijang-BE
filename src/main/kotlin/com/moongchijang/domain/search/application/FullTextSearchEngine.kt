package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedItemResponse
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.infrastructure.search.FullTextQueryBuilder
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.application.dto.SearchResponse
import com.moongchijang.domain.search.domain.SearchUiState
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * ngram FULLTEXT 인덱스 기반 검색 엔진.
 * BOOLEAN MODE 쿼리로 변환한 토큰이 하나도 없으면 검색을 스킵한다.
 */
@Component
class FullTextSearchEngine(
    private val groupBuyRepository: GroupBuyRepository,
) {
    companion object {
        private const val DEFAULT_LIMIT = 50
    }

    fun search(query: String): SearchResponse {
        val booleanQuery = FullTextQueryBuilder.toBooleanQuery(query)
        if (booleanQuery.isEmpty()) {
            return emptyResponse()
        }

        val ids = groupBuyRepository.searchIdsByFullText(
            query = booleanQuery,
            status = GroupBuyStatus.IN_PROGRESS.name,
            now = LocalDateTime.now(),
            limit = DEFAULT_LIMIT,
        )
        if (ids.isEmpty()) {
            return emptyResponse()
        }

        // store fetch join + 1차 쿼리의 deadline ASC 순서 보존
        val byId = groupBuyRepository.findAllWithStoreByIdIn(ids).associateBy { it.id }
        val matches = ids.mapNotNull { byId[it] }

        return SearchResponse(
            searchCase = SearchCase.NONE_DETECTED,
            detectedRegion = null,
            detectedProduct = null,
            confidence = 0.0,
            uiState = SearchUiState.RESULTS,
            totalCount = matches.size,
            results = matches.map { GroupBuyFeedItemResponse.from(it) },
        )
    }

    private fun emptyResponse() = SearchResponse(
        searchCase = SearchCase.NONE_DETECTED,
        detectedRegion = null,
        detectedProduct = null,
        confidence = 0.0,
        uiState = SearchUiState.EMPTY_CAN_REQUEST,
        totalCount = 0,
        results = emptyList(),
    )
}
