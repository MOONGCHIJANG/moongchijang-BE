package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.infrastructure.search.FullTextQueryBuilder
import com.moongchijang.domain.search.application.dto.GroupBuyCardDto
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.application.dto.SearchResponse
import com.moongchijang.domain.search.domain.SearchUiState
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * ngram FULLTEXT 인덱스만 사용하는 단순 검색 엔진.
 *
 * 기존 SearchOrchestrator (Gemini 키워드 추출 + Qdrant 벡터 + reranker) 와 달리
 * 외부 호출 없이 BOOLEAN MODE 쿼리만으로 결과를 반환한다.
 * 검색어가 BOOLEAN MODE 로 변환했을 때 토큰이 하나도 없으면 검색을 스킵한다.
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

        val matches = groupBuyRepository.searchByFullText(
            query = booleanQuery,
            status = GroupBuyStatus.IN_PROGRESS.name,
            now = LocalDateTime.now(),
            limit = DEFAULT_LIMIT,
        )

        return SearchResponse(
            searchCase = SearchCase.NONE_DETECTED,
            detectedRegion = null,
            detectedProduct = null,
            confidence = 0.0,
            uiState = if (matches.isEmpty()) SearchUiState.EMPTY_CAN_REQUEST else SearchUiState.RESULTS,
            totalCount = matches.size,
            results = matches.map { GroupBuyCardDto.from(it) },
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
