package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "search")
data class SearchProperties(
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
