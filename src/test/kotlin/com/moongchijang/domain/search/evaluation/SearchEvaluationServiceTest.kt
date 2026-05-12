package com.moongchijang.domain.search.evaluation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class SearchEvaluationServiceTest {

    private val service = SearchEvaluationService()

    @Test
    fun `evaluate는 골든 케이스 쿼리마다 프로바이더를 호출한다`() {
        val goldenCases = listOf(
            GoldenSearchCase("성수 두쫀쿠", setOf(1L)),
            GoldenSearchCase("홍대 소금빵", setOf(2L))
        )
        val invoked = mutableListOf<String>()
        val provider = SearchCandidateProvider { query ->
            invoked += query
            when (query) {
                "성수 두쫀쿠" -> listOf(1L)
                "홍대 소금빵" -> listOf(2L, 99L)
                else -> emptyList()
            }
        }

        val report = service.evaluate(goldenCases, provider, k = 10)

        assertThat(invoked).containsExactly("성수 두쫀쿠", "홍대 소금빵")
        assertThat(report.caseCount).isEqualTo(2)
        assertThat(report.recallAtK).isEqualTo(1.0, within(1e-9))
        assertThat(report.mrr).isEqualTo(1.0, within(1e-9))
    }

    @Test
    fun `evaluate는 사용자 지정 K를 메트릭 계산에 전달한다`() {
        val goldenCases = listOf(GoldenSearchCase("쿼리", setOf(5L)))
        val provider = SearchCandidateProvider { listOf(1L, 2L, 3L, 4L, 5L) }

        val reportK3 = service.evaluate(goldenCases, provider, k = 3)
        val reportK5 = service.evaluate(goldenCases, provider, k = 5)

        assertThat(reportK3.recallAtK).isZero
        assertThat(reportK5.recallAtK).isEqualTo(1.0, within(1e-9))
    }
}
