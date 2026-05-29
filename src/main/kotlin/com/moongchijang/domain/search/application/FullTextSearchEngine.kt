package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedItemResponse
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.infrastructure.search.FullTextQueryBuilder
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.application.dto.SearchResponse
import com.moongchijang.domain.search.domain.SearchUiState
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * ngram FULLTEXT 인덱스 기반 검색 엔진.
 *
 * 두 개의 FULLTEXT 인덱스(상품명 / 매장·주소)에 각각 매칭 여부를 확인해
 * `SearchCase` 4분기(BOTH/PRODUCT_ONLY/NEIGHBORHOOD_ONLY/NONE_DETECTED) 를 복원한다.
 *
 * 1차: strict AND 쿼리(`+token*`)로 정확 매칭 우선.
 * 2차: 1차에서 양쪽 인덱스 모두 0건이면 fallback OR 쿼리로 재조회. 합성어/부분 매칭 케이스를 잡는다.
 *
 * 1·2차 모두 0건이거나 사용자 입력이 빈 토큰뿐이면 [emptyResponse] 를 반환한다.
 */
@Component
class FullTextSearchEngine(
    private val groupBuyRepository: GroupBuyRepository,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
) {
    companion object {
        private const val DEFAULT_LIMIT = 50
        private const val CONFIDENCE_BOTH = 1.0
        private const val CONFIDENCE_SINGLE = 0.5
        private const val CONFIDENCE_NONE = 0.0
    }

    fun search(query: String): SearchResponse {
        val now = LocalDateTime.now()

        val (strictQuery, fallbackQuery) = FullTextQueryBuilder.buildQueries(query)
        if (strictQuery.isEmpty()) {
            return emptyResponse()
        }

        var productIds = searchProductIds(strictQuery, now)
        var storeIds = searchStoreIds(strictQuery, now)

        if (productIds.isEmpty() && storeIds.isEmpty()) {
            productIds = searchProductIds(fallbackQuery, now)
            storeIds = searchStoreIds(fallbackQuery, now)
        }

        val mergedIds = mergeIds(productIds, storeIds)
        if (mergedIds.isEmpty()) {
            return emptyResponse()
        }

        // store fetch join 후 deadline ASC 재정렬 (두 인덱스 결과를 한 줄로 머지)
        val matches = groupBuyRepository.findAllWithStoreByIdIn(mergedIds)
            .sortedBy { it.deadline }
            .take(DEFAULT_LIMIT)

        val productHit = productIds.isNotEmpty()
        val storeHit = storeIds.isNotEmpty()
        val trimmedQuery = query.trim()

        return SearchResponse(
            searchCase = classifyCase(productHit, storeHit),
            detectedRegion = trimmedQuery.takeIf { storeHit },
            detectedProduct = trimmedQuery.takeIf { productHit },
            confidence = confidenceOf(productHit, storeHit),
            uiState = SearchUiState.RESULTS,
            totalCount = matches.size,
            results = matches.map {
                GroupBuyFeedItemResponse.from(
                    groupBuy = it,
                    thumbnailUrl = s3ImageReferenceResolver.resolveForRead(it.thumbnailKey),
                    now = now,
                )
            },
        )
    }

    private fun searchProductIds(booleanQuery: String, now: LocalDateTime): List<Long> =
        if (booleanQuery.isEmpty()) emptyList()
        else groupBuyRepository.searchProductIdsByFullText(
            query = booleanQuery,
            status = GroupBuyStatus.IN_PROGRESS.name,
            now = now,
            limit = DEFAULT_LIMIT,
        )

    private fun searchStoreIds(booleanQuery: String, now: LocalDateTime): List<Long> =
        if (booleanQuery.isEmpty()) emptyList()
        else groupBuyRepository.searchStoreIdsByFullText(
            query = booleanQuery,
            status = GroupBuyStatus.IN_PROGRESS.name,
            now = now,
            limit = DEFAULT_LIMIT,
        )

    private fun mergeIds(productIds: List<Long>, storeIds: List<Long>): Set<Long> =
        productIds.union(storeIds)

    private fun classifyCase(productHit: Boolean, storeHit: Boolean): SearchCase = when {
        productHit && storeHit -> SearchCase.BOTH_DETECTED
        productHit -> SearchCase.PRODUCT_ONLY
        storeHit -> SearchCase.NEIGHBORHOOD_ONLY
        else -> SearchCase.NONE_DETECTED
    }

    private fun confidenceOf(productHit: Boolean, storeHit: Boolean): Double = when {
        productHit && storeHit -> CONFIDENCE_BOTH
        productHit || storeHit -> CONFIDENCE_SINGLE
        else -> CONFIDENCE_NONE
    }

    private fun emptyResponse() = SearchResponse(
        searchCase = SearchCase.NONE_DETECTED,
        detectedRegion = null,
        detectedProduct = null,
        confidence = CONFIDENCE_NONE,
        uiState = SearchUiState.EMPTY_CAN_REQUEST,
        totalCount = 0,
        results = emptyList(),
    )
}
