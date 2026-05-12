package com.moongchijang.support.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GoldenSearchCaseLoaderTest {

    @Test
    fun `loadDefault는 골든 CSV를 파싱해 GoldenSearchCase 리스트를 반환한다`() {
        val cases = GoldenSearchCaseLoader.loadDefault()

        assertThat(cases).isNotEmpty
        assertThat(cases.first().query).isEqualTo("성수 두쫀쿠")
        assertThat(cases.first().relevantGroupBuyIds).containsExactly(1L)
    }

    @Test
    fun `loadDefault는 빈 relevant 컬럼을 no-result expected 케이스로 인식한다`() {
        val cases = GoldenSearchCaseLoader.loadDefault()
        val noResultCases = cases.filter { it.relevantGroupBuyIds.isEmpty() }

        assertThat(noResultCases).isNotEmpty
        assertThat(noResultCases.map { it.query })
            .contains("강남 두쫀쿠", "asdfqwer1234")
    }

    @Test
    fun `loadDefault는 파이프 구분 relevant ID를 모두 파싱한다`() {
        val cases = GoldenSearchCaseLoader.loadDefault()
        val mangwon = cases.first { it.query == "망원" }

        assertThat(mangwon.relevantGroupBuyIds).containsExactlyInAnyOrder(4L, 5L)
    }
}
