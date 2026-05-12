package com.moongchijang.domain.search.evaluation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class SearchProviderComparisonTest {

    private val runner = SearchProviderComparisonRunner()

    @Test
    fun `compare는 각 프로바이더별 평가 리포트를 입력 순서대로 반환한다`() {
        val goldenCases = listOf(
            GoldenSearchCase("성수 두쫀쿠", setOf(1L)),
            GoldenSearchCase("홍대 소금빵", setOf(2L))
        )
        val providers = linkedMapOf<String, SearchCandidateProvider>(
            "perfect" to SearchCandidateProvider { query ->
                when (query) {
                    "성수 두쫀쿠" -> listOf(1L)
                    "홍대 소금빵" -> listOf(2L)
                    else -> emptyList()
                }
            },
            "broken" to SearchCandidateProvider { emptyList() }
        )

        val report = runner.compare(goldenCases, providers)

        assertThat(report.evaluations).hasSize(2)
        assertThat(report.evaluations.map { it.providerName })
            .containsExactly("perfect", "broken")
        assertThat(report.byProvider("perfect")?.report?.recallAtK)
            .isEqualTo(1.0, within(1e-9))
        assertThat(report.byProvider("broken")?.report?.recallAtK).isZero
        assertThat(report.byProvider("broken")?.report?.zeroResultRate)
            .isEqualTo(1.0, within(1e-9))
    }

    @Test
    fun `byProvider는 존재하지 않는 이름에 null을 반환한다`() {
        val report = SearchProviderComparisonReport(evaluations = emptyList())

        assertThat(report.byProvider("missing")).isNull()
    }
}
