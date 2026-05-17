package com.moongchijang.domain.groupbuy.infrastructure.search

/**
 * 사용자 검색어를 MySQL FULLTEXT BOOLEAN MODE 쿼리 문자열로 변환한다.
 *
 * - BOOLEAN MODE 연산자 문자(+, -, >, <, (, ), ~, *, ", @, \)는 사용자 입력에서 제거한다.
 * - 토큰별 prefix wildcard(`*`)와 필수(`+`) 연산자를 붙여 모든 토큰이 매칭되는 AND 의미를 만든다.
 * - ngram_token_size(기본 2) 미만의 한 글자 토큰은 매칭되지 않으므로 제외한다.
 *
 * 모든 토큰이 제거되면 빈 문자열을 반환한다. 호출 측에서 빈 문자열이면 검색을 스킵해야 한다.
 */
object FullTextQueryBuilder {

    private val FORBIDDEN_CHARS = Regex("""[+\-><()~*"@\\]""")
    private val WHITESPACE = Regex("""\s+""")
    private const val MIN_TOKEN_LENGTH = 2

    fun toBooleanQuery(rawQuery: String): String {
        val tokens = rawQuery
            .replace(FORBIDDEN_CHARS, " ")
            .trim()
            .split(WHITESPACE)
            .filter { it.length >= MIN_TOKEN_LENGTH }
        return tokens.joinToString(" ") { "+$it*" }
    }
}
