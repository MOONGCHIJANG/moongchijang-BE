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
     * 1차 strict 쿼리.
     * 토큰별 prefix wildcard(`*`)와 필수(`+`) 연산자를 붙여 모든 토큰이 매칭되는 AND 의미를 만든다.
     */
    fun toBooleanQuery(rawQuery: String): String {
        val tokens = sanitize(rawQuery)
        return tokens.joinToString(" ") { "+$it*" }
    }

    /**
     * 2차 fallback 쿼리. 1차에서 0건일 때 사용한다.
     *
     * - `+` 연산자를 제거해 OR 의미로 결합한다. 토큰 중 일부만 포함된 문서도 매치된다.
     * - 단일 토큰이고 길이가 ngram_token_size 를 초과하면 2글자 ngram 으로 분해해 결합한다.
     *   예: "카레소시지" → "카레 레소 소시 시지". 합성어 한 덩어리 입력에서도 부분 매칭이 가능해진다.
     */
    fun toFallbackQuery(rawQuery: String): String {
        val tokens = sanitize(rawQuery)
        if (tokens.isEmpty()) return ""
        if (tokens.size == 1 && tokens[0].length > NGRAM_TOKEN_SIZE) {
            return decomposeToNgrams(tokens[0]).joinToString(" ")
        }
        return tokens.joinToString(" ")
    }

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
