package com.moongchijang.domain.search.infrastructure.demo

import dev.langchain4j.model.chat.ChatModel
import com.fasterxml.jackson.databind.ObjectMapper

class LocalDemoChatModel(
    private val objectMapper: ObjectMapper
) : ChatModel {
    override fun chat(prompt: String): String {
        val extraction = LocalDemoSearchVocabulary.detectFromPrompt(prompt)
        val payload = mapOf(
            "neighborhood" to extraction.region,
            "product" to extraction.product
        )
        return objectMapper.writeValueAsString(payload)
    }
}
