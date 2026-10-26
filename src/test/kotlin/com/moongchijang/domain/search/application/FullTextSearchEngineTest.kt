package com.moongchijang.domain.search.application

import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.search.application.dto.SearchCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FullTextSearchEngineTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var searchCorrectionService: SearchCorrectionService

    private val engine by lazy { FullTextSearchEngine(productRepository, searchCorrectionService) }

    @Test
    fun `토큰이 없으면 상품 저장소를 조회하지 않는다`() {
        val result = engine.search("+-*()")

        assertThat(result).isEqualTo(ProductSearchMatch.empty())
        Mockito.verifyNoInteractions(productRepository)
    }

    @Test
    fun `상품명과 매장 검색 결과를 합치고 검색 유형을 분류한다`() {
        stubProductIds(listOf(1L, 2L))
        stubStoreProductIds(listOf(2L, 3L))

        val result = engine.search("성수 소금빵")

        assertThat(result.searchCase).isEqualTo(SearchCase.BOTH_DETECTED)
        assertThat(result.detectedProduct).isEqualTo("성수 소금빵")
        assertThat(result.detectedRegion).isEqualTo("성수 소금빵")
        assertThat(result.confidence).isEqualTo(1.0)
        assertThat(result.productIds).containsExactlyInAnyOrder(1L, 2L, 3L)
        Mockito.verifyNoInteractions(searchCorrectionService)
    }

    @Test
    fun `상품명만 검색되면 상품 검색으로 분류한다`() {
        stubProductIds(listOf(1L))
        stubStoreProductIds(emptyList())

        val result = engine.search("소금빵")

        assertThat(result.searchCase).isEqualTo(SearchCase.PRODUCT_ONLY)
        assertThat(result.detectedProduct).isEqualTo("소금빵")
        assertThat(result.detectedRegion).isNull()
        assertThat(result.productIds).containsExactly(1L)
    }

    @Test
    fun `매장만 검색되면 지역 검색으로 분류한다`() {
        stubProductIds(emptyList())
        stubStoreProductIds(listOf(2L))

        val result = engine.search("성수")

        assertThat(result.searchCase).isEqualTo(SearchCase.NEIGHBORHOOD_ONLY)
        assertThat(result.detectedProduct).isNull()
        assertThat(result.detectedRegion).isEqualTo("성수")
        assertThat(result.productIds).containsExactly(2L)
    }

    @Test
    fun `엄격 검색이 비면 완화 검색을 수행한다`() {
        stubProductIds(emptyList(), listOf(3L))
        stubStoreProductIds(emptyList(), emptyList())

        val result = engine.search("카레소시지")

        assertThat(result.productIds).containsExactly(3L)
        Mockito.verify(productRepository, Mockito.times(2))
            .searchProductIdsByFullText(anyString(), anyString(), anyString())
        Mockito.verify(productRepository, Mockito.times(2))
            .searchStoreProductIdsByFullText(anyString(), anyString(), anyString())
    }

    @Test
    fun `검색 결과가 없으면 보정어로 한 번 더 검색한다`() {
        stubProductIds(emptyList(), emptyList(), listOf(4L))
        stubStoreProductIds(emptyList(), emptyList(), emptyList())
        Mockito.`when`(searchCorrectionService.correct("카래")).thenReturn("카레")

        val result = engine.search("카래")

        assertThat(result.detectedProduct).isEqualTo("카레")
        assertThat(result.productIds).containsExactly(4L)
        Mockito.verify(searchCorrectionService).correct("카래")
    }

    @Test
    fun `보정어도 없으면 빈 검색 결과를 반환한다`() {
        stubProductIds(emptyList(), emptyList())
        stubStoreProductIds(emptyList(), emptyList())
        Mockito.`when`(searchCorrectionService.correct("없는상품")).thenReturn(null)

        val result = engine.search("없는상품")

        assertThat(result).isEqualTo(ProductSearchMatch.empty())
    }

    private fun stubProductIds(vararg results: List<Long>) {
        val stubbing = Mockito.`when`(
            productRepository.searchProductIdsByFullText(anyString(), anyString(), anyString())
        )
        results.fold(stubbing) { current, result -> current.thenReturn(result) }
    }

    private fun stubStoreProductIds(vararg results: List<Long>) {
        val stubbing = Mockito.`when`(
            productRepository.searchStoreProductIdsByFullText(anyString(), anyString(), anyString())
        )
        results.fold(stubbing) { current, result -> current.thenReturn(result) }
    }
}
