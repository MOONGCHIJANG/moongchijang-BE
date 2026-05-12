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

    // 단순 CSV 포맷: 필드 내부 콤마/따옴표/개행 미지원. relevant_ids는 파이프(|)로 다중값.
    // 향후 query에 콤마가 필요해지면 정식 CSV 라이브러리 도입을 검토한다.
    private fun parse(line: String): GoldenSearchCase {
        val parts = line.split(",", limit = 3)
        require(parts.size >= 2) { "잘못된 골든 케이스 줄: $line" }
        val query = parts[0]
        require(!query.contains('"') && !query.contains('\n')) {
            "query 필드에 따옴표/개행은 지원하지 않습니다: $query"
        }
        val relevantIds = parts[1]
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toLong() }
            .toSet()
        return GoldenSearchCase(query = query, relevantGroupBuyIds = relevantIds)
    }
}
