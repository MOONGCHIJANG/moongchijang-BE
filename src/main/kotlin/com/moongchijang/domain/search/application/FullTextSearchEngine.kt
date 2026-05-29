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
 *
 * 1차: strict AND 쿼리(`+token*`)로 정확 매칭 우선.
 * 2차: 1차에서 0건이면 fallback OR 쿼리로 재조회. 합성어/부분 매칭 케이스를 잡는다.
 *
 * 1·2차 모두 0건이거나 사용자 입력이 빈 토큰뿐이면 [emptyResponse] 를 반환한다.
 */
@Component
class FullTextSearchEngine(
    private val groupBuyRepository: GroupBuyRepository,
) {
    companion object {
        private const val DEFAULT_LIMIT = 50
    }

    fun search(query: String): SearchResponse {
        val now = LocalDateTime.now()

        val strictQuery = FullTextQueryBuilder.toBooleanQuery(query)
        val strictIds = if (strictQuery.isEmpty()) emptyList() else searchIds(strictQuery, now)

        val ids = strictIds.ifEmpty {
            val fallbackQuery = FullTextQueryBuilder.toFallbackQuery(query)
            if (fallbackQuery.isEmpty()) emptyList() else searchIds(fallbackQuery, now)
        }

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
            results = matches.map { GroupBuyFeedItemResponse.from(it, now) },
        )
    }

    private fun searchIds(booleanQuery: String, now: LocalDateTime): List<Long> =
        groupBuyRepository.searchIdsByFullText(
            query = booleanQuery,
            status = GroupBuyStatus.IN_PROGRESS.name,
            now = now,
            limit = DEFAULT_LIMIT,
        )

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
