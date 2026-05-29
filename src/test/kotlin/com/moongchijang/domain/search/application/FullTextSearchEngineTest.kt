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

class FullTextSearchEngineTest {

    private val groupBuyRepository: GroupBuyRepository = Mockito.mock(GroupBuyRepository::class.java)
    private val s3ImageReferenceResolver: S3ImageReferenceResolver = Mockito.mock(S3ImageReferenceResolver::class.java)
    private val engine = FullTextSearchEngine(groupBuyRepository, s3ImageReferenceResolver)

    private fun stubIds(ids: List<Long>) {
        Mockito.`when`(
            groupBuyRepository.searchIdsByFullText(
                anyString(),
                anyString(),
                anyLocalDateTime(),
                anyInt(),
            )
        ).thenReturn(ids)
    }

    private fun stubFetch(ids: Collection<Long>, matches: List<GroupBuy>) {
        Mockito.`when`(
            groupBuyRepository.findAllWithStoreByIdIn(ids)
        ).thenReturn(matches)
    }

    @Test
    @DisplayName("토큰이 하나도 없으면 Repository를 호출하지 않고 EMPTY_CAN_REQUEST를 반환한다")
    fun `empty boolean query short circuits without repository call`() {
        val response = engine.search("+-*()")

        assertThat(response.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(response.totalCount).isZero
        assertThat(response.results).isEmpty()
        assertThat(response.searchCase).isEqualTo(SearchCase.NONE_DETECTED)
        Mockito.verifyNoInteractions(groupBuyRepository)
    }

    @Test
    @DisplayName("매칭 id가 있으면 fetch join 결과를 1차 쿼리 순서대로 RESULTS 카드 DTO로 반환한다")
    fun `non empty id list produces RESULTS with mapped cards in id order`() {
        val first = SearchTestFixtures.groupBuy(id = 10L, productName = "소금빵")
        val second = SearchTestFixtures.groupBuy(id = 20L, productName = "크루아상")
        stubIds(listOf(10L, 20L))
        // fetch 결과는 의도적으로 역순 — 1차 id 순서로 재정렬되는지 검증
        stubFetch(listOf(10L, 20L), listOf(second, first))

        val response = engine.search("소금빵")

        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.totalCount).isEqualTo(2)
        assertThat(response.results.map { it.id }).containsExactly(10L, 20L)
        assertThat(response.detectedRegion).isNull()
        assertThat(response.detectedProduct).isNull()
    }

    @Test
    @DisplayName("매칭 id가 0개면 fetch를 호출하지 않고 EMPTY_CAN_REQUEST를 반환한다")
    fun `empty id list short circuits fetch`() {
        stubIds(emptyList())

        val response = engine.search("존재하지않는상품")

        assertThat(response.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(response.totalCount).isZero
        assertThat(response.results).isEmpty()
        Mockito.verify(groupBuyRepository, Mockito.never()).findAllWithStoreByIdIn(anyLongList())
    }
}
