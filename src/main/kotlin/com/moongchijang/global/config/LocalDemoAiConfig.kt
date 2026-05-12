package com.moongchijang.global.config

import com.moongchijang.domain.search.infrastructure.demo.LocalDemoChatModel
import com.moongchijang.domain.search.infrastructure.demo.LocalDemoEmbeddingModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import tools.jackson.databind.ObjectMapper

@Configuration
@Profile("local-demo")
class LocalDemoAiConfig(
    private val objectMapper: ObjectMapper
) {
    @Bean
    fun chatModel(): ChatModel = LocalDemoChatModel(objectMapper)

    @Bean
    fun embeddingModel(): EmbeddingModel = LocalDemoEmbeddingModel()
}
