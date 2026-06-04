package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.domain.SearchUiState
import com.moongchijang.global.util.S3ImageReferenceResolver
import com.moongchijang.support.search.MockitoKotlinMatchers.anyLocalDateTime
import com.moongchijang.support.search.MockitoKotlinMatchers.anyLongList
import com.moongchijang.support.search.SearchTestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDateTime

class FullTextSearchEngineTest {

    private val groupBuyRepository: GroupBuyRepository = Mockito.mock(GroupBuyRepository::class.java)
    private val s3ImageReferenceResolver: S3ImageReferenceResolver = Mockito.mock(S3ImageReferenceResolver::class.java)
    private val searchCorrectionService: SearchCorrectionService = Mockito.mock(SearchCorrectionService::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-22T01:00:00Z"), ZoneOffset.UTC)
    private val engine = FullTextSearchEngine(groupBuyRepository, s3ImageReferenceResolver, searchCorrectionService, clock)

    private fun stubProductIds(vararg sequentialReturns: List<Long>) {
        val stubbing = Mockito.`when`(
            groupBuyRepository.searchProductIdsByFullText(anyString(), anyString(), anyLocalDateTime(), anyInt())
        )
        sequentialReturns.fold(stubbing) { acc, ret -> acc.thenReturn(ret) }
    }

    private fun stubStoreIds(vararg sequentialReturns: List<Long>) {
        val stubbing = Mockito.`when`(
            groupBuyRepository.searchStoreIdsByFullText(anyString(), anyString(), anyLocalDateTime(), anyInt())
        )
        sequentialReturns.fold(stubbing) { acc, ret -> acc.thenReturn(ret) }
    }

    private fun stubFetch(matches: List<GroupBuy>) {
        Mockito.`when`(groupBuyRepository.findAllWithStoreByIdIn(Mockito.anyCollection()))
            .thenReturn(matches)
    }

    @Test
    @DisplayName("토큰이 하나도 없으면 Repository를 호출하지 않고 EMPTY_CAN_REQUEST를 반환한다")
    fun `empty boolean query short circuits without repository call`() {
        val response = engine.search("+-*()")

        assertThat(response.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(response.totalCount).isZero
        assertThat(response.results).isEmpty()
        assertThat(response.searchCase).isEqualTo(SearchCase.NONE_DETECTED)
        assertThat(response.confidence).isEqualTo(0.0)
        Mockito.verifyNoInteractions(groupBuyRepository)
    }

    @Test
    @DisplayName("상품 인덱스만 hit 하면 PRODUCT_ONLY 로 분류하고 detectedProduct 에 검색어를 채운다")
    fun `product only hit yields PRODUCT_ONLY case`() {
        val saltBread = SearchTestFixtures.groupBuy(id = 10L, productName = "소금빵")
        stubProductIds(listOf(10L))
        stubStoreIds(emptyList())
        stubFetch(listOf(saltBread))

        val response = engine.search("소금빵")

        assertThat(response.searchCase).isEqualTo(SearchCase.PRODUCT_ONLY)
        assertThat(response.detectedProduct).isEqualTo("소금빵")
        assertThat(response.detectedRegion).isNull()
        assertThat(response.confidence).isEqualTo(0.5)
        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.results.map { it.id }).containsExactly(10L)
        Mockito.verifyNoInteractions(searchCorrectionService)
    }

    @Test
    @DisplayName("매장/주소 인덱스만 hit 하면 NEIGHBORHOOD_ONLY 로 분류하고 detectedRegion 에 검색어를 채운다")
    fun `store only hit yields NEIGHBORHOOD_ONLY case`() {
        val seongsuStoreGroupBuy = SearchTestFixtures.groupBuy(id = 20L, productName = "베이글")
        stubProductIds(emptyList())
        stubStoreIds(listOf(20L))
        stubFetch(listOf(seongsuStoreGroupBuy))

        val response = engine.search("성수")

        assertThat(response.searchCase).isEqualTo(SearchCase.NEIGHBORHOOD_ONLY)
        assertThat(response.detectedRegion).isEqualTo("성수")
        assertThat(response.detectedProduct).isNull()
        assertThat(response.confidence).isEqualTo(0.5)
        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.results.map { it.id }).containsExactly(20L)
        Mockito.verifyNoInteractions(searchCorrectionService)
    }

    @Test
    @DisplayName("양쪽 인덱스가 모두 hit 하면 BOTH_DETECTED 로 분류하고 detectedProduct/Region 모두 채운다")
    fun `both indexes hit yields BOTH_DETECTED case`() {
        val saltBread = SearchTestFixtures.groupBuy(
            id = 10L,
            productName = "소금빵",
            deadline = LocalDateTime.now().plusDays(1),
        )
        val seongsuBagel = SearchTestFixtures.groupBuy(
            id = 20L,
            productName = "베이글",
            deadline = LocalDateTime.now().plusDays(2),
        )
        stubProductIds(listOf(10L))
        stubStoreIds(listOf(20L))
        stubFetch(listOf(saltBread, seongsuBagel))

        val response = engine.search("성수 소금빵")

        assertThat(response.searchCase).isEqualTo(SearchCase.BOTH_DETECTED)
        assertThat(response.detectedProduct).isEqualTo("성수 소금빵")
        assertThat(response.detectedRegion).isEqualTo("성수 소금빵")
        assertThat(response.confidence).isEqualTo(1.0)
        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.totalCount).isEqualTo(2)
        // deadline ASC 정렬 검증 (saltBread = +1일, seongsuBagel = +2일)
        assertThat(response.results.map { it.id }).containsExactly(10L, 20L)
        Mockito.verifyNoInteractions(searchCorrectionService)
    }

    @Test
    @DisplayName("양쪽 인덱스에서 동일한 id 가 반환되면 dedupe 되어 한 번만 결과에 포함된다")
    fun `overlapping ids are deduplicated`() {
        val sharedHit = SearchTestFixtures.groupBuy(id = 10L, productName = "성수 소금빵")
        stubProductIds(listOf(10L))
        stubStoreIds(listOf(10L))
        stubFetch(listOf(sharedHit))

        val response = engine.search("성수 소금빵")

        assertThat(response.searchCase).isEqualTo(SearchCase.BOTH_DETECTED)
        assertThat(response.totalCount).isEqualTo(1)
        assertThat(response.results.map { it.id }).containsExactly(10L)
    }

    @Test
    @DisplayName("1차 strict 양쪽 모두 0건이고 2차 fallback 도 모두 0건이면 NONE_DETECTED + EMPTY_CAN_REQUEST")
    fun `all empty yields NONE_DETECTED and EMPTY_CAN_REQUEST`() {
        stubProductIds(emptyList(), emptyList())
        stubStoreIds(emptyList(), emptyList())

        val response = engine.search("존재하지않는상품")

        assertThat(response.searchCase).isEqualTo(SearchCase.NONE_DETECTED)
        assertThat(response.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(response.confidence).isEqualTo(0.0)
        assertThat(response.detectedProduct).isNull()
        assertThat(response.detectedRegion).isNull()
        Mockito.verify(groupBuyRepository, Mockito.never()).findAllWithStoreByIdIn(anyLongList())
    }

    @Test
    @DisplayName("1차 strict 가 한쪽이라도 hit 하면 2차 fallback 쿼리는 실행되지 않는다")
    fun `strict any hit skips fallback`() {
        val saltBread = SearchTestFixtures.groupBuy(id = 10L, productName = "소금빵")
        stubProductIds(listOf(10L))
        stubStoreIds(emptyList())
        stubFetch(listOf(saltBread))

        engine.search("소금빵")

        Mockito.verify(groupBuyRepository, Mockito.times(1))
            .searchProductIdsByFullText(anyString(), anyString(), anyLocalDateTime(), anyInt())
        Mockito.verify(groupBuyRepository, Mockito.times(1))
            .searchStoreIdsByFullText(anyString(), anyString(), anyLocalDateTime(), anyInt())
    }

    @Test
    @DisplayName("1차 strict 양쪽 0건이면 2차 fallback 쿼리로 양쪽 인덱스를 재조회한다")
    fun `strict miss falls back on both axes`() {
        val curry = SearchTestFixtures.groupBuy(id = 30L, productName = "카레")
        val sausage = SearchTestFixtures.groupBuy(id = 40L, productName = "소시지")
        stubProductIds(emptyList(), listOf(30L, 40L))
        stubStoreIds(emptyList(), emptyList())
        stubFetch(listOf(curry, sausage))

        val response = engine.search("카레소시지")

        assertThat(response.searchCase).isEqualTo(SearchCase.PRODUCT_ONLY)
        assertThat(response.totalCount).isEqualTo(2)
        assertThat(response.results.map { it.id }).containsExactlyInAnyOrder(30L, 40L)
        Mockito.verify(groupBuyRepository, Mockito.times(2))
            .searchProductIdsByFullText(anyString(), anyString(), anyLocalDateTime(), anyInt())
        Mockito.verify(groupBuyRepository, Mockito.times(2))
            .searchStoreIdsByFullText(anyString(), anyString(), anyLocalDateTime(), anyInt())
        Mockito.verifyNoInteractions(searchCorrectionService)
    }

    @Test
    @DisplayName("strict/fallback 검색 결과가 0건이면 보정어로 한 번 더 FULLTEXT 검색한다")
    fun `empty first pass searches once more with corrected query`() {
        val groupBuy = SearchTestFixtures.groupBuy(id = 30L, productName = "카레라면")
        stubProductIds(emptyList(), emptyList(), listOf(30L))
        stubStoreIds(emptyList(), emptyList(), emptyList())
        Mockito.`when`(searchCorrectionService.correct("카래")).thenReturn("카레")
        stubFetch(listOf(groupBuy))

        val response = engine.search("카래")

        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.searchCase).isEqualTo(SearchCase.PRODUCT_ONLY)
        assertThat(response.detectedProduct).isEqualTo("카레")
        assertThat(response.totalCount).isEqualTo(1)
        assertThat(response.results.map { it.productName }).containsExactly("카레라면")
        Mockito.verify(searchCorrectionService).correct("카래")
        Mockito.verify(groupBuyRepository, Mockito.times(3))
            .searchProductIdsByFullText(anyString(), anyString(), anyLocalDateTime(), anyInt())
        Mockito.verify(groupBuyRepository, Mockito.times(3))
            .searchStoreIdsByFullText(anyString(), anyString(), anyLocalDateTime(), anyInt())
    }

    @Test
    @DisplayName("보정어가 없으면 EMPTY_CAN_REQUEST 응답을 그대로 반환한다")
    fun `empty first pass without correction remains empty`() {
        stubProductIds(emptyList(), emptyList())
        stubStoreIds(emptyList(), emptyList())
        Mockito.`when`(searchCorrectionService.correct("없는상품")).thenReturn(null)

        val response = engine.search("없는상품")

        assertThat(response.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(response.totalCount).isZero
        Mockito.verify(searchCorrectionService).correct("없는상품")
        Mockito.verify(groupBuyRepository, Mockito.never()).findAllWithStoreByIdIn(anyLongList())
    }
}
