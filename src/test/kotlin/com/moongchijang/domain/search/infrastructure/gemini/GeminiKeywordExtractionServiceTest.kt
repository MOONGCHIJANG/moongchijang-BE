package com.moongchijang.domain.search.infrastructure.gemini

import com.moongchijang.domain.search.application.dto.SearchCase
import dev.langchain4j.model.chat.ChatModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import tools.jackson.module.kotlin.jacksonObjectMapper

class GeminiKeywordExtractionServiceTest {

    private val chatModel: ChatModel = Mockito.mock(ChatModel::class.java)
    private val objectMapper = jacksonObjectMapper()
    private val service = GeminiKeywordExtractionService(chatModel, objectMapper)

    private val validRegions = listOf("성수", "홍대", "연남")
    private val validProducts = listOf("두쫀쿠", "소금빵")

    @Test
    fun `유효 동네 + 유효 상품이면 BOTH_DETECTED를 반환한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenReturn("""{"neighborhood":"성수","product":"두쫀쿠"}""")

        val result = service.extract("성수 두쫀쿠", validRegions, validProducts)

        assertThat(result.region).isEqualTo("성수")
        assertThat(result.product).isEqualTo("두쫀쿠")
        assertThat(result.searchCase).isEqualTo(SearchCase.BOTH_DETECTED)
    }

    @Test
    fun `상품만 있으면 PRODUCT_ONLY를 반환한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenReturn("""{"neighborhood":null,"product":"소금빵"}""")

        val result = service.extract("소금빵", validRegions, validProducts)

        assertThat(result.searchCase).isEqualTo(SearchCase.PRODUCT_ONLY)
        assertThat(result.product).isEqualTo("소금빵")
        assertThat(result.region).isNull()
    }

    @Test
    fun `동네만 있으면 NEIGHBORHOOD_ONLY를 반환한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenReturn("""{"neighborhood":"홍대","product":null}""")

        val result = service.extract("홍대", validRegions, validProducts)

        assertThat(result.searchCase).isEqualTo(SearchCase.NEIGHBORHOOD_ONLY)
        assertThat(result.region).isEqualTo("홍대")
        assertThat(result.product).isNull()
    }

    @Test
    fun `둘 다 null이면 NONE_DETECTED를 반환한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenReturn("""{"neighborhood":null,"product":null}""")

        val result = service.extract("asdfqwer", validRegions, validProducts)

        assertThat(result.searchCase).isEqualTo(SearchCase.NONE_DETECTED)
    }

    @Test
    fun `JSON 앞뒤에 텍스트가 있어도 객체를 추출해 파싱한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenReturn("```json\n{\"neighborhood\":\"성수\",\"product\":\"두쫀쿠\"}\n```")

        val result = service.extract("성수 두쫀쿠", validRegions, validProducts)

        assertThat(result.searchCase).isEqualTo(SearchCase.BOTH_DETECTED)
    }

    @Test
    fun `LLM이 목록 외 동네를 반환하면 서버에서 null로 재검증한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenReturn("""{"neighborhood":"강남","product":"두쫀쿠"}""")

        val result = service.extract("강남 두쫀쿠", validRegions, validProducts)

        assertThat(result.region).isNull()
        assertThat(result.product).isEqualTo("두쫀쿠")
        assertThat(result.searchCase).isEqualTo(SearchCase.PRODUCT_ONLY)
    }

    @Test
    fun `LLM이 목록 외 상품을 반환하면 서버에서 null로 재검증한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenReturn("""{"neighborhood":"성수","product":"마카롱"}""")

        val result = service.extract("성수 마카롱", validRegions, validProducts)

        assertThat(result.region).isEqualTo("성수")
        assertThat(result.product).isNull()
        assertThat(result.searchCase).isEqualTo(SearchCase.NEIGHBORHOOD_ONLY)
    }

    @Test
    fun `chatModel이 예외를 던지면 NONE_DETECTED로 폴백한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenThrow(RuntimeException("Gemini 호출 실패"))

        val result = service.extract("성수 두쫀쿠", validRegions, validProducts)

        assertThat(result.searchCase).isEqualTo(SearchCase.NONE_DETECTED)
        assertThat(result.region).isNull()
        assertThat(result.product).isNull()
    }

    @Test
    fun `JSON 객체가 없는 응답은 NONE_DETECTED로 폴백한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenReturn("자연어로만 응답합니다")

        val result = service.extract("성수 두쫀쿠", validRegions, validProducts)

        assertThat(result.searchCase).isEqualTo(SearchCase.NONE_DETECTED)
    }

    @Test
    fun `프롬프트에 오타 약어 유사 발음 보정 지시를 포함한다`() {
        Mockito.`when`(chatModel.chat(Mockito.anyString()))
            .thenReturn("""{"neighborhood":null,"product":null}""")

        service.extract("두쫀크크", validRegions, validProducts)

        val promptCaptor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(chatModel).chat(promptCaptor.capture())
        assertThat(promptCaptor.value).contains("상품 오타, 약어, 유사 발음")
        assertThat(promptCaptor.value).contains("유효 상품 목록 중 가장 가까운 값")
    }
}
