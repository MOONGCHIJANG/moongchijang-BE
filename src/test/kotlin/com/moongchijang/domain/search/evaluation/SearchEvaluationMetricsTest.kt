package com.moongchijang.domain.search.evaluation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import kotlin.math.ln

class SearchEvaluationMetricsTest {

    private val metrics = SearchEvaluationMetrics()

    @Test
    fun `recallAtK는 top-K 안의 관련 항목 수를 관련 항목 총합으로 나눈다`() {
        val rankedIds = listOf(1L, 2L, 3L, 4L, 5L)
        val relevantIds = setOf(2L, 5L, 99L)

        assertThat(metrics.recallAtK(rankedIds, relevantIds, k = 10))
            .isEqualTo(2.0 / 3.0, within(1e-9))
    }

    @Test
    fun `recallAtK는 K 이후 항목을 카운트하지 않는다`() {
        val rankedIds = listOf(1L, 2L, 3L, 4L, 5L)
        val relevantIds = setOf(5L)

        assertThat(metrics.recallAtK(rankedIds, relevantIds, k = 3)).isEqualTo(0.0)
    }

    @Test
    fun `recallAtK는 관련 항목이 없으면 0을 반환한다`() {
        assertThat(metrics.recallAtK(listOf(1L, 2L), emptySet(), k = 10)).isEqualTo(0.0)
    }

    @Test
    fun `precisionAtK는 top-K 안의 관련 항목 수를 K로 나눈다`() {
        val rankedIds = listOf(1L, 2L, 3L, 4L, 5L)
        val relevantIds = setOf(2L, 3L)

        assertThat(metrics.precisionAtK(rankedIds, relevantIds, k = 5))
            .isEqualTo(0.4, within(1e-9))
    }

    @Test
    fun `precisionAtK는 관련 항목이 없으면 0을 반환한다`() {
        assertThat(metrics.precisionAtK(listOf(1L, 2L, 3L), emptySet(), k = 3)).isEqualTo(0.0)
    }

    @Test
    fun `reciprocalRank는 첫 관련 항목의 역순위를 반환한다`() {
        val rankedIds = listOf(10L, 20L, 30L)
        val relevantIds = setOf(20L)

        assertThat(metrics.reciprocalRank(rankedIds, relevantIds))
            .isEqualTo(0.5, within(1e-9))
    }

    @Test
    fun `reciprocalRank는 첫 순위가 관련일 때 1을 반환한다`() {
        assertThat(metrics.reciprocalRank(listOf(1L, 2L), setOf(1L))).isEqualTo(1.0)
    }

    @Test
    fun `reciprocalRank는 관련 항목이 없거나 매칭이 없으면 0을 반환한다`() {
        assertThat(metrics.reciprocalRank(listOf(1L, 2L), emptySet())).isEqualTo(0.0)
        assertThat(metrics.reciprocalRank(listOf(1L, 2L), setOf(99L))).isEqualTo(0.0)
    }

    @Test
    fun `ndcgAtK는 첫 위치 관련 항목에 가장 높은 이득을 부여한다`() {
        val rankedIds = listOf(2L, 1L, 3L)
        val relevantIds = setOf(2L)

        assertThat(metrics.ndcgAtK(rankedIds, relevantIds, k = 3)).isEqualTo(1.0, within(1e-9))
    }

    @Test
    fun `ndcgAtK는 후순위 관련 항목에 낮은 점수를 부여한다`() {
        val rankedIds = listOf(1L, 2L, 3L)
        val relevantIds = setOf(2L)

        val expected = (1.0 / log2(3.0)) / (1.0 / log2(2.0))
        assertThat(metrics.ndcgAtK(rankedIds, relevantIds, k = 3)).isEqualTo(expected, within(1e-9))
    }

    @Test
    fun `ndcgAtK는 관련 항목이 없으면 0을 반환한다`() {
        assertThat(metrics.ndcgAtK(listOf(1L, 2L, 3L), emptySet(), k = 3)).isEqualTo(0.0)
    }

    @Test
    fun `evaluate는 빈 골든 케이스에 대해 0 리포트를 반환한다`() {
        val report = metrics.evaluate(emptyList(), emptyMap())

        assertThat(report.caseCount).isZero
        assertThat(report.recallAtK).isZero
        assertThat(report.precisionAtK).isZero
        assertThat(report.mrr).isZero
        assertThat(report.zeroResultRate).isZero
        assertThat(report.falsePositiveRate).isZero
    }

    @Test
    fun `evaluate는 positive와 no-result-expected 케이스를 분리해 집계한다`() {
        val goldenCases = listOf(
            GoldenSearchCase("성수 두쫀쿠", setOf(1L)),
            GoldenSearchCase("홍대 소금빵", setOf(2L, 20L)),
            GoldenSearchCase("강남 두쫀쿠", emptySet()),
            GoldenSearchCase("xyz nonsense", emptySet())
        )
        val ranked = mapOf(
            "성수 두쫀쿠" to listOf(1L, 99L, 100L),
            "홍대 소금빵" to listOf(2L, 20L, 30L),
            "강남 두쫀쿠" to emptyList(),
            "xyz nonsense" to listOf(7L, 8L)
        )

        val report = metrics.evaluate(goldenCases, ranked, k = 10)

        assertThat(report.caseCount).isEqualTo(4)
        assertThat(report.positiveCaseCount).isEqualTo(2)
        assertThat(report.noResultExpectedCaseCount).isEqualTo(2)
        assertThat(report.k).isEqualTo(10)
        assertThat(report.recallAtK).isEqualTo(1.0, within(1e-9))
        assertThat(report.mrr).isEqualTo(1.0, within(1e-9))
        assertThat(report.falsePositiveCount).isEqualTo(1)
        assertThat(report.falsePositiveRate).isEqualTo(0.5, within(1e-9))
        assertThat(report.zeroResultRate).isEqualTo(0.25, within(1e-9))
    }

    @Test
    fun `evaluate는 누락된 쿼리를 빈 결과로 처리한다`() {
        val goldenCases = listOf(GoldenSearchCase("두쫀쿠", setOf(1L)))
        val report = metrics.evaluate(goldenCases, emptyMap(), k = 10)

        assertThat(report.recallAtK).isZero
        assertThat(report.precisionAtK).isZero
        assertThat(report.zeroResultRate).isEqualTo(1.0, within(1e-9))
    }

    private fun log2(value: Double): Double = ln(value) / ln(2.0)
}
