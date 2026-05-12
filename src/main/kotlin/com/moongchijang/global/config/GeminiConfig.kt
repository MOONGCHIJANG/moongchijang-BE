package com.moongchijang.global.config

import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!local-demo")
class GeminiConfig(
    @Value("\${gemini.api-key}") private val apiKey: String
) {
    @Bean
    fun chatLanguageModel(): ChatModel =
        GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gemini-2.0-flash")
            .temperature(0.1)
            .build()

    @Bean
    fun embeddingModel(): EmbeddingModel =
        GoogleAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName("gemini-embedding-001")
            .outputDimensionality(768)
            .build()
}
