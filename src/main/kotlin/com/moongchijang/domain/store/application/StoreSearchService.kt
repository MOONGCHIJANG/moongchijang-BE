package com.moongchijang.domain.store.application

import com.moongchijang.domain.store.application.dto.StoreSearchResponse
import com.moongchijang.domain.store.infrastructure.naver.NaverLocalSearchClient
import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchItem
import org.springframework.stereotype.Service

@Service
class StoreSearchService(
    private val naverLocalSearchClient: NaverLocalSearchClient
) {
    companion object {
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
        val response = naverLocalSearchClient.search(keyword, display)
        return StoreSearchResponse.from(response.items.filter { it.isBakeryDomain() })
    }

    private fun NaverLocalSearchItem.isBakeryDomain(): Boolean {
        val normalizedCategory = category.lowercase()
        val normalizedStoreName = storeName().lowercase()

        return BAKERY_CATEGORY_KEYWORDS.any { normalizedCategory.contains(it.lowercase()) } ||
            BAKERY_STORE_NAME_KEYWORDS.any { normalizedStoreName.contains(it.lowercase()) }
    }
}
