package com.moongchijang.domain.search.infrastructure

/** 사용자 검색어를 MySQL FULLTEXT BOOLEAN MODE 쿼리 문자열로 변환한다. */
object FullTextQueryBuilder {

    private val FORBIDDEN_CHARS = Regex("""[+\-><()~*"@\\]""")
    private val WHITESPACE = Regex("""\s+""")
    private const val MIN_TOKEN_LENGTH = 2
    private const val NGRAM_TOKEN_SIZE = 2

    fun buildQueries(rawQuery: String): Pair<String, String> {
        val tokens = sanitize(rawQuery)
        if (tokens.isEmpty()) {
            return "" to ""
        }
        val strict = tokens.joinToString(" ") { "+$it*" }
        val fallback = tokens.flatMap { token ->
            if (token.length > NGRAM_TOKEN_SIZE) decomposeToNgrams(token) else listOf(token)
        }.joinToString(" ")
        return strict to fallback
    }

    fun toBooleanQuery(rawQuery: String): String = buildQueries(rawQuery).first

    fun toFallbackQuery(rawQuery: String): String = buildQueries(rawQuery).second

    private fun sanitize(rawQuery: String): List<String> = rawQuery
        .replace(FORBIDDEN_CHARS, " ")
        .trim()
        .split(WHITESPACE)
        .filter { it.length >= MIN_TOKEN_LENGTH }

    private fun decomposeToNgrams(token: String): List<String> =
        (0..token.length - NGRAM_TOKEN_SIZE).map { token.substring(it, it + NGRAM_TOKEN_SIZE) }
}
