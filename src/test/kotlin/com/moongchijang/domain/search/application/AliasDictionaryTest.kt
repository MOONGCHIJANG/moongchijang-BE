package com.moongchijang.domain.search.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AliasDictionaryTest {

    private val aliasDictionary = AliasDictionary()

    @Test
    fun `유효 상품을 query가 직접 포함하면 그 상품을 반환한다`() {
        val validProducts = listOf("소금빵", "두쫀쿠")

        assertThat(aliasDictionary.resolveProduct("성수 소금빵 추천", validProducts)).isEqualTo("소금빵")
    }

    @Test
    fun `대소문자 차이가 있어도 매칭한다`() {
        val validProducts = listOf("Croissant")

        assertThat(aliasDictionary.resolveProduct("croissant 맛집", validProducts)).isEqualTo("Croissant")
    }

    @Test
    fun `alias 매핑이 있고 정식명이 유효 목록에 있으면 정식명을 반환한다`() {
        val validProducts = listOf("소금빵")

        assertThat(aliasDictionary.resolveProduct("시오빵 먹고싶다", validProducts)).isEqualTo("소금빵")
        assertThat(aliasDictionary.resolveProduct("salt bread please", validProducts)).isEqualTo("소금빵")
    }

    @Test
    fun `alias 정식명이 유효 목록에 없으면 null을 반환한다`() {
        val validProducts = listOf("두쫀쿠")

        assertThat(aliasDictionary.resolveProduct("시오빵", validProducts)).isNull()
    }

    @Test
    fun `isAliasMatch는 query에 alias가 있고 정식명이 일치할 때 true`() {
        assertThat(aliasDictionary.isAliasMatch("시오빵 추천", "소금빵")).isTrue
        assertThat(aliasDictionary.isAliasMatch("두쫀쿠", "소금빵")).isFalse
        assertThat(aliasDictionary.isAliasMatch("시오빵", null)).isFalse
    }
}
