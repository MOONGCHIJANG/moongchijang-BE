package com.moongchijang.domain.search.infrastructure.gemini

import com.moongchijang.domain.search.application.dto.KeywordExtractionResult
import com.moongchijang.domain.search.application.dto.SearchCase
import dev.langchain4j.model.chat.ChatModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class GeminiKeywordExtractionService(
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private data class ExtractionResponse(
        val neighborhood: String? = null,
        val product: String? = null
    )

    fun extract(
        query: String,
        validRegions: List<String>,
        validProducts: List<String>
    ): KeywordExtractionResult {
        return try {
            val prompt = buildPrompt(query, validRegions, validProducts)
            val json = chatModel.chat(prompt)
            parseResult(json, validRegions, validProducts)
        } catch (e: Exception) {
            log.warn("키워드 추출 실패, NONE_DETECTED 폴백: query={}, error={}", query, e.message)
            KeywordExtractionResult(null, null, SearchCase.NONE_DETECTED)
        }
    }

    private fun buildPrompt(query: String, validRegions: List<String>, validProducts: List<String>): String =
        """
        아래 규칙에 따라 사용자 검색어에서 동네와 상품을 추출하세요.

        규칙:
        - 반드시 유효 목록에 있는 값만 반환하세요.
        - 목록에 없으면 null을 반환하세요.
        - 절대 목록 외 값을 생성하지 마세요.
        - JSON 형식으로만 응답하세요: {"neighborhood": "...", "product": "..."}

        유효 동네: ${validRegions.joinToString(", ")}
        유효 상품: ${validProducts.joinToString(", ")}

        사용자 검색어: "$query"
        """.trimIndent()

    private fun parseResult(
        json: String,
        validRegions: List<String>,
        validProducts: List<String>
    ): KeywordExtractionResult {
        val cleanJson = extractJsonObject(json)
        val parsed = objectMapper.readValue(cleanJson, ExtractionResponse::class.java)

        // 서버 사이드 재검증: 유효 목록에 없으면 null 처리
        val neighborhood = parsed.neighborhood?.takeIf { it in validRegions }
        val product = parsed.product?.takeIf { it in validProducts }

        val searchCase = when {
            neighborhood != null && product != null -> SearchCase.BOTH_DETECTED
            product != null -> SearchCase.PRODUCT_ONLY
            neighborhood != null -> SearchCase.NEIGHBORHOOD_ONLY
            else -> SearchCase.NONE_DETECTED
        }

        return KeywordExtractionResult(neighborhood, product, searchCase)
    }

    private fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start in 0 until end) { "JSON object not found in LLM response" }
        return raw.substring(start, end + 1)
    }
}
