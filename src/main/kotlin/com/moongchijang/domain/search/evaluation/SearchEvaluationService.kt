package com.moongchijang.domain.search.evaluation

fun interface SearchCandidateProvider {
    fun retrieveGroupBuyIds(query: String): List<Long>
}

class SearchEvaluationService(
    private val metrics: SearchEvaluationMetrics = SearchEvaluationMetrics()
) {
    fun evaluate(
        goldenCases: List<GoldenSearchCase>,
        provider: SearchCandidateProvider,
        k: Int = 10
    ): SearchEvaluationReport {
        val results = goldenCases.associate { goldenCase ->
            goldenCase.query to provider.retrieveGroupBuyIds(goldenCase.query)
        }
        return metrics.evaluate(goldenCases, results, k)
    }
}
