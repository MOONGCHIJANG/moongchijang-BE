package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "search")
data class SearchProperties(
    /**
     * 검색 엔진 선택 플래그.
     * - "legacy": 기존 Gemini + Qdrant 하이브리드 경로 (SearchOrchestrator)
     * - "fulltext": ngram FULLTEXT 단일 경로 (FullTextSearchEngine)
     * 단계적 전환을 위해 도입. 안정화 후 별도 PR 에서 legacy 코드와 함께 제거.
     */
    val engine: String = "legacy",
    val retrieval: Retrieval = Retrieval(),
    val guard: Guard = Guard(),
    val observability: Observability = Observability()
) {
    data class Retrieval(
        val vectorCandidateLimit: Int = 10,
        val vectorMinScore: Double = 0.5,
        val fallbackProvider: String = "mysql"
    )

    data class Guard(
        val enabled: Boolean = true,
        val minConfidence: Double = 0.65,
        val supportiveScoreThreshold: Double = 0.7
    )

    data class Observability(
        val enabled: Boolean = true
    )
}
