package com.moongchijang.domain.search.application

import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.infrastructure.FullTextQueryBuilder
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 상품 카탈로그와 판매 매장을 대상으로 검색 후보를 찾는다.
 *
 * 상품명과 매장명·주소의 FULLTEXT 인덱스를 분리 조회해 기존 검색 분류를 유지하고,
 * 두 결과의 합집합을 상품 피드 조회 조건으로 전달한다.
 */
@Component
class FullTextSearchEngine(
    private val productRepository: ProductRepository,
    private val searchCorrectionService: SearchCorrectionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(query: String): ProductSearchMatch {
        val startedAt = System.nanoTime()
        val response = searchByFullText(query)
        if (response.productIds.isNotEmpty()) {
            return response
        }

        val normalizedQuery = SearchQueryNormalizer.normalize(query)
        val correctedQuery = searchCorrectionService.correct(query)
            ?.takeIf { it != normalizedQuery }
            ?: return response.also {
                logEmptyResult(
                    query = query,
                    normalizedQuery = normalizedQuery,
                    correctedQuery = null,
                    startedAt = startedAt,
                )
            }

        val correctedResponse = searchByFullText(correctedQuery)
        log.info(
            "search_correction fallback=true originalQuery={} normalizedQuery={} correctedQuery={} resultCount={} latencyMs={}",
            query,
            normalizedQuery,
            correctedQuery,
            correctedResponse.productIds.size,
            elapsedMillis(startedAt),
        )
        return correctedResponse
    }

    private fun searchByFullText(query: String): ProductSearchMatch {
        val (strictQuery, fallbackQuery) = FullTextQueryBuilder.buildQueries(query)
        if (strictQuery.isEmpty()) {
            return ProductSearchMatch.empty()
        }

        var productIds = searchProductIds(strictQuery)
        var storeProductIds = searchStoreProductIds(strictQuery)

        if (productIds.isEmpty() && storeProductIds.isEmpty()) {
            productIds = searchProductIds(fallbackQuery)
            storeProductIds = searchStoreProductIds(fallbackQuery)
        }

        val productHit = productIds.isNotEmpty()
        val storeHit = storeProductIds.isNotEmpty()
        val trimmedQuery = query.trim()

        return ProductSearchMatch(
            searchCase = classifyCase(productHit, storeHit),
            detectedRegion = trimmedQuery.takeIf { storeHit },
            detectedProduct = trimmedQuery.takeIf { productHit },
            confidence = confidenceOf(productHit, storeHit),
            productIds = productIds.union(storeProductIds),
        )
    }

    private fun searchProductIds(booleanQuery: String): List<Long> =
        if (booleanQuery.isEmpty()) {
            emptyList()
        } else {
            productRepository.searchProductIdsByFullText(
                query = booleanQuery,
                productStatus = ProductStatus.ACTIVE.name,
                storeSalesStatus = StoreSalesStatus.ACTIVE.name,
            )
        }

    private fun searchStoreProductIds(booleanQuery: String): List<Long> =
        if (booleanQuery.isEmpty()) {
            emptyList()
        } else {
            productRepository.searchStoreProductIdsByFullText(
                query = booleanQuery,
                productStatus = ProductStatus.ACTIVE.name,
                storeSalesStatus = StoreSalesStatus.ACTIVE.name,
            )
        }

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

    private fun logEmptyResult(
        query: String,
        normalizedQuery: String,
        correctedQuery: String?,
        startedAt: Long,
    ) {
        log.info(
            "search_correction fallback=false originalQuery={} normalizedQuery={} correctedQuery={} resultCount=0 latencyMs={}",
            query,
            normalizedQuery,
            correctedQuery,
            elapsedMillis(startedAt),
        )
    }

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / 1_000_000

    companion object {
        private const val CONFIDENCE_BOTH = 1.0
        private const val CONFIDENCE_SINGLE = 0.5
        private const val CONFIDENCE_NONE = 0.0
    }
}

data class ProductSearchMatch(
    val searchCase: SearchCase,
    val detectedRegion: String?,
    val detectedProduct: String?,
    val confidence: Double,
    val productIds: Set<Long>,
) {
    companion object {
        fun empty() = ProductSearchMatch(
            searchCase = SearchCase.NONE_DETECTED,
            detectedRegion = null,
            detectedProduct = null,
            confidence = 0.0,
            productIds = emptySet(),
        )
    }
}
