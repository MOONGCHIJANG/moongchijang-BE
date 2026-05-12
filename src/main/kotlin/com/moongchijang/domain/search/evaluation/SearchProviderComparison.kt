package com.moongchijang.domain.search.evaluation

data class SearchProviderEvaluation(
    val providerName: String,
    val report: SearchEvaluationReport
)

data class SearchProviderComparisonReport(
    val evaluations: List<SearchProviderEvaluation>
) {
    fun byProvider(providerName: String): SearchProviderEvaluation? =
        evaluations.firstOrNull { it.providerName == providerName }
}

class SearchProviderComparisonRunner(
    private val evaluationService: SearchEvaluationService = SearchEvaluationService()
) {
    fun compare(
        goldenCases: List<GoldenSearchCase>,
        providers: Map<String, SearchCandidateProvider>,
        k: Int = 10
    ): SearchProviderComparisonReport =
        SearchProviderComparisonReport(
            evaluations = providers.map { (providerName, provider) ->
                SearchProviderEvaluation(
                    providerName = providerName,
                    report = evaluationService.evaluate(goldenCases, provider, k)
                )
            }
        )
}
