package com.moongchijang.domain.search.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProductNormalizerTest {

    private val normalizer = ProductNormalizer(AliasDictionary())

    @Test
    fun `Gemini가 유효 상품을 반환하면 그대로 사용한다`() {
        val validProducts = listOf("소금빵", "두쫀쿠")

        assertThat(normalizer.normalize("성수 두쫀쿠", "두쫀쿠", validProducts)).isEqualTo("두쫀쿠")
    }

    @Test
    fun `alias 매핑으로 canonical 상품명을 반환한다`() {
        val validProducts = listOf("소금빵", "두쫀쿠")

        assertThat(normalizer.normalize("시오빵 추천", null, validProducts)).isEqualTo("소금빵")
    }

    @Test
    fun `토큰 기반 fuzzy match로 오타를 canonical 상품명으로 보정한다`() {
        val validProducts = listOf("두쫀쿠", "마들렌", "크루아상", "소금빵")

        assertThat(normalizer.normalize("성수 두쫀크크", null, validProducts)).isEqualTo("두쫀쿠")
        assertThat(normalizer.normalize("마들랜 먹고싶다", null, validProducts)).isEqualTo("마들렌")
        assertThat(normalizer.normalize("크로와상", null, validProducts)).isEqualTo("크루아상")
    }

    @Test
    fun `1글자 token은 fuzzy match 대상에서 제외한다`() {
        val validProducts = listOf("빵")

        assertThat(normalizer.normalize("빵", null, validProducts)).isEqualTo("빵")
        assertThat(normalizer.normalize("ㅋ", null, validProducts)).isNull()
    }

    @Test
    fun `threshold 미만의 먼 문자열은 null을 반환한다`() {
        val validProducts = listOf("두쫀쿠", "마들렌", "크루아상", "소금빵")

        assertThat(normalizer.normalize("강남 마카롱", null, validProducts)).isNull()
        assertThat(normalizer.normalize("이태원 피자", null, validProducts)).isNull()
        assertThat(normalizer.normalize("없는상품", null, validProducts)).isNull()
    }

    @Test
    fun `1등과 2등 후보 점수 차이가 작으면 null을 반환한다`() {
        val validProducts = listOf("마들렌", "마들랜")

        assertThat(normalizer.normalize("마들렝", null, validProducts)).isNull()
    }
}
