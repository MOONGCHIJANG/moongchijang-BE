package com.moongchijang.domain.search.application

import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.domain.SearchIntent
import com.moongchijang.domain.search.domain.SearchUiState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SearchDecisionEngineTest {

    private val engine = SearchDecisionEngine()

    @Test
    fun `결과가 1건 이상이면 항상 RESULTS를 반환한다`() {
        val intent = SearchIntent(region = null, product = null, searchCase = SearchCase.NONE_DETECTED, confidence = 0.0)

        assertThat(engine.decide(intent, resultCount = 3)).isEqualTo(SearchUiState.RESULTS)
    }

    @Test
    fun `결과가 0건이고 BOTH_DETECTED면 EMPTY_CAN_REQUEST`() {
        val intent = SearchIntent(region = "서울", product = "두쫀쿠", searchCase = SearchCase.BOTH_DETECTED, confidence = 0.9)

        assertThat(engine.decide(intent, 0)).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
    }

    @Test
    fun `결과가 0건이고 PRODUCT_ONLY면 NEED_REGION`() {
        val intent = SearchIntent(region = null, product = "두쫀쿠", searchCase = SearchCase.PRODUCT_ONLY, confidence = 0.65)

        assertThat(engine.decide(intent, 0)).isEqualTo(SearchUiState.NEED_REGION)
    }

    @Test
    fun `결과가 0건이고 NEIGHBORHOOD_ONLY면 NEED_PRODUCT`() {
        val intent = SearchIntent(region = "서울", product = null, searchCase = SearchCase.NEIGHBORHOOD_ONLY, confidence = 0.65)

        assertThat(engine.decide(intent, 0)).isEqualTo(SearchUiState.NEED_PRODUCT)
    }

    @Test
    fun `결과가 0건이고 NONE_DETECTED면 NEED_BOTH`() {
        val intent = SearchIntent(region = null, product = null, searchCase = SearchCase.NONE_DETECTED, confidence = 0.0)

        assertThat(engine.decide(intent, 0)).isEqualTo(SearchUiState.NEED_BOTH)
    }
}
