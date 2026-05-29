package com.moongchijang.domain.store.application

import com.moongchijang.domain.store.application.dto.StoreSearchResponse
import com.moongchijang.domain.store.infrastructure.naver.NaverLocalSearchClient
import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchItem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StoreSearchService(
    private val naverLocalSearchClient: NaverLocalSearchClient
) {
    private val log = LoggerFactory.getLogger(StoreSearchService::class.java)
    companion object {
        private const val NAVER_FETCH_DISPLAY = 20

        private val BAKERY_CATEGORY_KEYWORDS = listOf(
            "베이커리",
            "제과",
            "디저트",
            "카페,디저트",
            "떡",
            "도넛",
            "한과",
        )

        private val BAKERY_STORE_NAME_KEYWORDS = listOf(
            "베이커리",
            "빵",
            "제과",
            "디저트",
            "도넛",
            "케이크",
            "쿠키",
            "마카롱",
            "타르트",
            "크루아상",
            "베이글",
            "떡",
            "한과",
            "찹쌀",
            "와플",
            "파이",
            "브레드",
            "bread",
            "bakery",
            "bake",
            "boulangerie",
            "patisserie",
            "dessert",
            "donut",
            "bagel",
            "cake",
        )
    }

    fun search(keyword: String, display: Int = 5): StoreSearchResponse {
        log.info("[StoreSearchService] 매장 검색 시작: keywordLength={}, display={}", keyword.length, display)
        if (keyword.isBlank()) {
            log.info("[StoreSearchService] 매장 검색 스킵: 빈 키워드")
            return StoreSearchResponse.from(emptyList())
        }
        val response = naverLocalSearchClient.search(keyword, display.coerceAtLeast(NAVER_FETCH_DISPLAY))
        val result = StoreSearchResponse.from(response.items.filter { it.isBakeryDomain() }.take(display))
        log.info("[StoreSearchService] 매장 검색 완료: resultCount={}", result.stores.size)
        return result
    }

    private fun NaverLocalSearchItem.isBakeryDomain(): Boolean {
        val normalizedCategory = category.lowercase()
        val normalizedStoreName = storeName().lowercase()

        return BAKERY_CATEGORY_KEYWORDS.any { normalizedCategory.contains(it) } ||
            BAKERY_STORE_NAME_KEYWORDS.any { normalizedStoreName.contains(it) }
    }
}
