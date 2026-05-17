package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.domain.SearchUiState
import com.moongchijang.support.search.MockitoKotlinMatchers.anyLocalDateTime
import com.moongchijang.support.search.SearchTestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito

class FullTextSearchEngineTest {

    private val groupBuyRepository: GroupBuyRepository = Mockito.mock(GroupBuyRepository::class.java)
    private val engine = FullTextSearchEngine(groupBuyRepository)

    private fun stubSearch(result: List<GroupBuy>) {
        Mockito.`when`(
            groupBuyRepository.searchByFullText(
                anyString(),
                anyString(),
                anyLocalDateTime(),
                anyInt(),
            )
        ).thenReturn(result)
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
    @DisplayName("매칭 결과가 있으면 RESULTS와 카드 DTO 리스트를 반환한다")
    fun `non empty match list produces RESULTS with mapped cards`() {
        val match = SearchTestFixtures.groupBuy(id = 1L, productName = "소금빵")
        stubSearch(listOf(match))

        val response = engine.search("소금빵")

        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.totalCount).isEqualTo(1)
        assertThat(response.results).hasSize(1)
        assertThat(response.results[0].id).isEqualTo(1L)
        assertThat(response.results[0].productName).isEqualTo("소금빵")
        assertThat(response.detectedRegion).isNull()
        assertThat(response.detectedProduct).isNull()
    }

    @Test
    @DisplayName("Repository가 빈 결과를 반환하면 EMPTY_CAN_REQUEST를 반환한다")
    fun `empty match list produces EMPTY_CAN_REQUEST`() {
        stubSearch(emptyList())

        val response = engine.search("존재하지않는상품")

        assertThat(response.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(response.totalCount).isZero
        assertThat(response.results).isEmpty()
    }

}
