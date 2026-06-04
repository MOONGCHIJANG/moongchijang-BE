package com.moongchijang.domain.store.application

import com.moongchijang.domain.store.infrastructure.naver.NaverLocalSearchClient
import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchItem
import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class StoreSearchServiceTest {

    private val naverLocalSearchClient: NaverLocalSearchClient = Mockito.mock(NaverLocalSearchClient::class.java)
    private val recommendedStoreImageService: RecommendedStoreImageService =
        Mockito.mock(RecommendedStoreImageService::class.java)
    private val service = StoreSearchService(
        naverLocalSearchClient,
        recommendedStoreImageService,
    )

    @BeforeEach
    fun setUp() {
        Mockito.`when`(recommendedStoreImageService.findActiveImageUrls())
            .thenReturn(
                listOf(
                    "https://dkg5euyknlpa.cloudfront.net/dev/recommended-store/1.jpeg",
                    "https://dkg5euyknlpa.cloudfront.net/dev/recommended-store/2.jpeg",
                )
            )
        Mockito.`when`(recommendedStoreImageService.imageUrlByIndex(Mockito.anyInt(), Mockito.anyList()))
            .thenAnswer {
                val index = it.getArgument<Int>(0)
                val imageUrls = it.getArgument<List<String>>(1)
                imageUrls[index.mod(imageUrls.size)]
            }
    }

    @Test
    fun `search returns only bakery domain stores from naver local results`() {
        Mockito.`when`(naverLocalSearchClient.search("헤어숍", 20)).thenReturn(
            response(
                item(title = "<b>성수베이커리</b>", category = "음식점>카페,디저트"),
                item(title = "강남헤어숍", category = "생활,편의>미용"),
                item(title = "잠실도넛", category = "음식점>카페"),
            )
        )

        val result = service.search("헤어숍")

        assertThat(result.stores).extracting("storeName")
            .containsExactly("성수베이커리", "잠실도넛")
        assertThat(result.stores).extracting("imageUrl")
            .containsExactly(
                "https://dkg5euyknlpa.cloudfront.net/dev/recommended-store/1.jpeg",
                "https://dkg5euyknlpa.cloudfront.net/dev/recommended-store/2.jpeg",
            )
    }

    @Test
    fun `search returns empty list when naver results are outside bakery domain`() {
        Mockito.`when`(naverLocalSearchClient.search("네일", 20)).thenReturn(
            response(
                item(title = "성수네일", category = "생활,편의>네일아트"),
                item(title = "홍대헤어", category = "생활,편의>미용"),
            )
        )

        val result = service.search("네일")

        assertThat(result.stores).isEmpty()
    }

    @Test
    fun `search fetches more naver results and returns requested display after filtering`() {
        Mockito.`when`(naverLocalSearchClient.search("베이글", 20)).thenReturn(
            response(
                item(title = "베이글1", category = "음식점>카페,디저트"),
                item(title = "베이글2", category = "음식점>카페,디저트"),
                item(title = "베이글3", category = "음식점>카페,디저트"),
                item(title = "베이글4", category = "음식점>카페,디저트"),
                item(title = "베이글5", category = "음식점>카페,디저트"),
                item(title = "베이글6", category = "음식점>카페,디저트"),
            )
        )

        val result = service.search("베이글", display = 5)

        assertThat(result.stores).extracting("storeName")
            .containsExactly("베이글1", "베이글2", "베이글3", "베이글4", "베이글5")
    }

    private fun response(vararg items: NaverLocalSearchItem) = NaverLocalSearchResponse(
        total = items.size,
        start = 1,
        display = items.size,
        items = items.toList(),
    )

    private fun item(
        title: String,
        category: String,
        link: String = "https://map.naver.com/p/entry/place/12345",
    ) = NaverLocalSearchItem(
        title = title,
        link = link,
        category = category,
        description = "",
        telephone = "",
        address = "서울 성동구",
        roadAddress = "서울 성동구 연무장길 1",
        mapx = "1270559610",
        mapy = "375445810",
    )
}
