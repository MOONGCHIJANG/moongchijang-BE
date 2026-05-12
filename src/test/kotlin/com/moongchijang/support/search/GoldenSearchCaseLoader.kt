package com.moongchijang.support.search

import com.moongchijang.domain.search.evaluation.GoldenSearchCase

object GoldenSearchCaseLoader {
    private const val DEFAULT_PATH = "/search/golden-search-cases.csv"

    fun loadDefault(): List<GoldenSearchCase> = load(DEFAULT_PATH)

    fun load(classpathResource: String): List<GoldenSearchCase> {
        val stream = checkNotNull(javaClass.getResourceAsStream(classpathResource)) {
            "golden case CSV를 찾을 수 없습니다: $classpathResource"
        }
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines
                .drop(1)
                .filter { it.isNotBlank() }
                .map(::parse)
                .toList()
        }
    }

    private fun parse(line: String): GoldenSearchCase {
        val parts = line.split(",", limit = 3)
        require(parts.size >= 2) { "잘못된 골든 케이스 줄: $line" }
        val query = parts[0]
        val relevantIds = parts[1]
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toLong() }
            .toSet()
        return GoldenSearchCase(query = query, relevantGroupBuyIds = relevantIds)
    }
}
