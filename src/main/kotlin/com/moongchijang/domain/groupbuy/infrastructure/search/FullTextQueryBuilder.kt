package com.moongchijang.domain.groupbuy.infrastructure.search

/**
 * 사용자 검색어를 MySQL FULLTEXT BOOLEAN MODE 쿼리 문자열로 변환한다.
 *
 * - BOOLEAN MODE 연산자 문자(+, -, >, <, (, ), ~, *, ", @, \)는 사용자 입력에서 제거한다.
 * - ngram_token_size(기본 2) 미만의 한 글자 토큰은 매칭되지 않으므로 제외한다.
 * - 1차 strict([toBooleanQuery]) 와 2차 fallback([toFallbackQuery]) 두 단계 쿼리를 제공한다.
 *
 * 모든 토큰이 제거되면 빈 문자열을 반환한다. 호출 측에서 빈 문자열이면 검색을 스킵해야 한다.
 */
object FullTextQueryBuilder {

    private val FORBIDDEN_CHARS = Regex("""[+\-><()~*"@\\]""")
    private val WHITESPACE = Regex("""\s+""")
    private const val MIN_TOKEN_LENGTH = 2
    private const val NGRAM_TOKEN_SIZE = 2

    /**
     * 1차 strict 쿼리와 2차 fallback 쿼리를 한 번의 sanitize 로 동시에 생성한다.
     *
     * - strict: 모든 토큰에 `+token*` 를 붙여 AND 의미로 결합.
     * - fallback: `+` 제거한 OR 결합. ngram_token_size 를 초과하는 토큰은 토큰별로 2글자 ngram 으로 분해.
     * - 토큰이 하나도 남지 않으면 두 쿼리 모두 빈 문자열을 반환한다.
     *   호출 측은 strict 가 빈 문자열이면 fallback 도 빈 문자열임을 가정할 수 있다.
     */
    fun buildQueries(rawQuery: String): Pair<String, String> {
        val tokens = sanitize(rawQuery)
        if (tokens.isEmpty()) return "" to ""
        val strict = tokens.joinToString(" ") { "+$it*" }
        val fallback = tokens.flatMap { token ->
            if (token.length > NGRAM_TOKEN_SIZE) decomposeToNgrams(token)
            else listOf(token)
        }.joinToString(" ")
        return strict to fallback
    }

    /** 1차 strict 쿼리 단독 호출용 wrapper. */
    fun toBooleanQuery(rawQuery: String): String = buildQueries(rawQuery).first

    /** 2차 fallback 쿼리 단독 호출용 wrapper. */
    fun toFallbackQuery(rawQuery: String): String = buildQueries(rawQuery).second

    private fun sanitize(rawQuery: String): List<String> {
        return rawQuery
            .replace(FORBIDDEN_CHARS, " ")
            .trim()
            .split(WHITESPACE)
            .filter { it.length >= MIN_TOKEN_LENGTH }
    }

    private fun decomposeToNgrams(token: String): List<String> {
        return (0..token.length - NGRAM_TOKEN_SIZE).map { token.substring(it, it + NGRAM_TOKEN_SIZE) }
    }
}
