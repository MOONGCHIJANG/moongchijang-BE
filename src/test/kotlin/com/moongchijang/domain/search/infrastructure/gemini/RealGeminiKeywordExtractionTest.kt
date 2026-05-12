package com.moongchijang.domain.search.infrastructure.gemini

import com.moongchijang.domain.search.application.dto.SearchCase
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 실제 Gemini API를 호출하는 통합 테스트.
 *
 * 실행 조건: 환경 변수 GEMINI_API_KEY 설정.
 * CI에서는 시크릿이 없으면 자동 스킵된다.
 *
 * 검증 목표:
 *  - 실제 LLM이 우리 프롬프트로 유효 목록 외 값을 절대 생성하지 않는다 (서버 재검증 외 보장).
 *  - 다양한 한국어 쿼리에서 적절한 SearchCase로 분류한다.
 */
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class RealGeminiKeywordExtractionTest {

    private val chatModel = GoogleAiGeminiChatModel.builder()
        .apiKey(System.getenv("GEMINI_API_KEY"))
        .modelName("gemini-2.0-flash")
        .temperature(0.1)
        .build()

    private val service = GeminiKeywordExtractionService(chatModel, jacksonObjectMapper())

    private val validRegions = listOf("성수", "홍대", "연남", "망원")
    private val validProducts = listOf("두쫀쿠", "소금빵", "크루아상", "마들렌")

    @Test
    fun `유효 동네와 유효 상품이 모두 포함된 쿼리는 BOTH_DETECTED로 분류된다`() {
        val result = service.extract("성수 두쫀쿠", validRegions, validProducts)

        assertThat(result.region).isEqualTo("성수")
        assertThat(result.product).isEqualTo("두쫀쿠")
        assertThat(result.searchCase).isEqualTo(SearchCase.BOTH_DETECTED)
    }

    @Test
    fun `목록 외 동네가 포함되면 region이 null이거나 서버 검증에 의해 null로 떨어진다`() {
        val result = service.extract("강남 두쫀쿠", validRegions, validProducts)

        // LLM이 강남을 반환하더라도 서버측 validRegions 재검증으로 null이 되어야 함
        assertThat(result.region).isNull()
    }

    @Test
    fun `목록 외 상품이 포함되면 product가 null로 보정된다`() {
        val result = service.extract("성수 마카롱", validRegions, validProducts)

        assertThat(result.product).isNull()
    }

    @Test
    fun `한국어 자연어 쿼리에서도 hallucination 없이 유효값만 반환한다`() {
        val result = service.extract("성수에서 맛있는 두쫀쿠 추천해줘", validRegions, validProducts)

        if (result.region != null) assertThat(result.region).isIn(validRegions)
        if (result.product != null) assertThat(result.product).isIn(validProducts)
    }

    @Test
    fun `완전 무의미한 쿼리는 NONE_DETECTED를 반환한다`() {
        val result = service.extract("asdfqwer1234", validRegions, validProducts)

        assertThat(result.region).isNull()
        assertThat(result.product).isNull()
        assertThat(result.searchCase).isEqualTo(SearchCase.NONE_DETECTED)
    }
}
