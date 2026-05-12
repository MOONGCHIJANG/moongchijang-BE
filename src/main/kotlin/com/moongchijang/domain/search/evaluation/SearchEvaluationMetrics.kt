package com.moongchijang.domain.search.evaluation

import kotlin.math.ln
import kotlin.math.min

data class GoldenSearchCase(
    val query: String,
    val relevantGroupBuyIds: Set<Long>
)

data class SearchEvaluationReport(
    val caseCount: Int,
    val positiveCaseCount: Int,
    val noResultExpectedCaseCount: Int,
    val k: Int,
    val recallAtK: Double,
    val precisionAtK: Double,
    val ndcgAtK: Double,
    val mrr: Double,
    val zeroResultRate: Double,
    val falsePositiveRate: Double,
    val falsePositiveCount: Int
)

class SearchEvaluationMetrics {
    fun evaluate(
        goldenCases: List<GoldenSearchCase>,
        rankedResultsByQuery: Map<String, List<Long>>,
        k: Int = 10
    ): SearchEvaluationReport {
        require(k > 0) { "k must be positive" }
        if (goldenCases.isEmpty()) {
            return SearchEvaluationReport(
                caseCount = 0,
                positiveCaseCount = 0,
                noResultExpectedCaseCount = 0,
                k = k,
                recallAtK = 0.0,
                precisionAtK = 0.0,
                ndcgAtK = 0.0,
                mrr = 0.0,
                zeroResultRate = 0.0,
                falsePositiveRate = 0.0,
                falsePositiveCount = 0
            )
        }

        val scores = goldenCases.map { goldenCase ->
            val rankedIds = rankedResultsByQuery[goldenCase.query].orEmpty()
            val noResultExpected = goldenCase.relevantGroupBuyIds.isEmpty()
            CaseScores(
                recallAtK = recallAtK(rankedIds, goldenCase.relevantGroupBuyIds, k),
                precisionAtK = precisionAtK(rankedIds, goldenCase.relevantGroupBuyIds, k),
                ndcgAtK = ndcgAtK(rankedIds, goldenCase.relevantGroupBuyIds, k),
                reciprocalRank = reciprocalRank(rankedIds, goldenCase.relevantGroupBuyIds),
                zeroResult = rankedIds.isEmpty(),
                noResultExpected = noResultExpected,
                falsePositive = noResultExpected && rankedIds.isNotEmpty()
            )
        }
        val positiveScores = scores.filterNot { it.noResultExpected }
        val noResultExpectedScores = scores.filter { it.noResultExpected }
        val falsePositiveCount = scores.count { it.falsePositive }

        return SearchEvaluationReport(
            caseCount = goldenCases.size,
            positiveCaseCount = positiveScores.size,
            noResultExpectedCaseCount = noResultExpectedScores.size,
            k = k,
            recallAtK = positiveScores.averageOfOrZero { it.recallAtK },
            precisionAtK = positiveScores.averageOfOrZero { it.precisionAtK },
            ndcgAtK = positiveScores.averageOfOrZero { it.ndcgAtK },
            mrr = positiveScores.averageOfOrZero { it.reciprocalRank },
            zeroResultRate = scores.count { it.zeroResult }.toDouble() / scores.size,
            falsePositiveRate = if (noResultExpectedScores.isEmpty()) 0.0 else falsePositiveCount.toDouble() / noResultExpectedScores.size,
            falsePositiveCount = falsePositiveCount
        )
    }

    fun recallAtK(rankedIds: List<Long>, relevantIds: Set<Long>, k: Int): Double {
        if (relevantIds.isEmpty()) return 0.0
        val hits = rankedIds.take(k).count { it in relevantIds }
        return hits.toDouble() / relevantIds.size
    }

    fun precisionAtK(rankedIds: List<Long>, relevantIds: Set<Long>, k: Int): Double {
        if (relevantIds.isEmpty()) return 0.0
        val hits = rankedIds.take(k).count { it in relevantIds }
        return hits.toDouble() / k
    }

    fun reciprocalRank(rankedIds: List<Long>, relevantIds: Set<Long>): Double {
        if (relevantIds.isEmpty()) return 0.0
        val firstRelevantIndex = rankedIds.indexOfFirst { it in relevantIds }
        return if (firstRelevantIndex >= 0) 1.0 / (firstRelevantIndex + 1) else 0.0
    }

    fun ndcgAtK(rankedIds: List<Long>, relevantIds: Set<Long>, k: Int): Double {
        if (relevantIds.isEmpty()) return 0.0
        val dcg = rankedIds.take(k).mapIndexed { index, id ->
            if (id in relevantIds) 1.0 / log2(index + 2.0) else 0.0
        }.sum()
        val idealHits = min(relevantIds.size, k)
        val idcg = (0 until idealHits).sumOf { index -> 1.0 / log2(index + 2.0) }
        return if (idcg == 0.0) 0.0 else dcg / idcg
    }

    private fun log2(value: Double): Double = ln(value) / ln(2.0)

    private fun List<CaseScores>.averageOfOrZero(selector: (CaseScores) -> Double): Double =
        if (isEmpty()) 0.0 else map(selector).average()

    private data class CaseScores(
        val recallAtK: Double,
        val precisionAtK: Double,
        val ndcgAtK: Double,
        val reciprocalRank: Double,
        val zeroResult: Boolean,
        val noResultExpected: Boolean,
        val falsePositive: Boolean
    )
}
