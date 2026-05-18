package com.moongchijang.domain.groupbuy.infrastructure.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FullTextQueryBuilderTest {

    @Test
    @DisplayName("단일 한글 토큰은 +token* 형태로 변환된다")
    fun `single korean token produces required prefix wildcard`() {
        val result = FullTextQueryBuilder.toBooleanQuery("소금빵")

        assertThat(result).isEqualTo("+소금빵*")
    }

    @Test
    @DisplayName("여러 토큰은 모두 +token* 형태로 AND 결합된다")
    fun `multiple tokens are joined as AND with required prefix wildcards`() {
        val result = FullTextQueryBuilder.toBooleanQuery("성수 소금빵")

        assertThat(result).isEqualTo("+성수* +소금빵*")
    }

    @Test
    @DisplayName("연속된 공백은 단일 구분자로 처리된다")
    fun `consecutive whitespace is collapsed`() {
        val result = FullTextQueryBuilder.toBooleanQuery("  성수   소금빵  ")

        assertThat(result).isEqualTo("+성수* +소금빵*")
    }

    @Test
    @DisplayName("빈 문자열은 빈 문자열로 반환된다")
    fun `empty input returns empty string`() {
        val result = FullTextQueryBuilder.toBooleanQuery("")

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("공백만 있는 입력은 빈 문자열로 반환된다")
    fun `whitespace only input returns empty string`() {
        val result = FullTextQueryBuilder.toBooleanQuery("   ")

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("한 글자 토큰은 ngram_token_size 미만이므로 제외된다")
    fun `single character tokens are filtered out`() {
        val result = FullTextQueryBuilder.toBooleanQuery("성 수 빵")

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("한 글자와 두 글자가 섞이면 두 글자 이상만 남는다")
    fun `mixed length tokens keep only tokens of length two or more`() {
        val result = FullTextQueryBuilder.toBooleanQuery("성 성수 빵")

        assertThat(result).isEqualTo("+성수*")
    }

    @Test
    @DisplayName("BOOLEAN MODE 연산자 문자는 제거된다")
    fun `boolean mode operator characters are stripped`() {
        val result = FullTextQueryBuilder.toBooleanQuery("성수+빵집*\"소금빵\"")

        assertThat(result).isEqualTo("+성수* +빵집* +소금빵*")
    }

    @Test
    @DisplayName("괄호와 물결 등 모든 금지 문자가 제거된다")
    fun `all forbidden characters are removed`() {
        val result = FullTextQueryBuilder.toBooleanQuery("(성수)~소금빵<홍대>@빵집\\카페")

        assertThat(result).isEqualTo("+성수* +소금빵* +홍대* +빵집* +카페*")
    }

    @Test
    @DisplayName("금지 문자만 입력되면 빈 문자열로 반환된다")
    fun `input containing only forbidden characters returns empty string`() {
        val result = FullTextQueryBuilder.toBooleanQuery("+-*()~\"")

        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("영문/숫자 토큰도 동일하게 변환된다")
    fun `latin and digit tokens are handled the same way`() {
        val result = FullTextQueryBuilder.toBooleanQuery("seongsu cafe 2024")

        assertThat(result).isEqualTo("+seongsu* +cafe* +2024*")
    }
}
