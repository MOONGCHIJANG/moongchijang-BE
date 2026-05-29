package com.moongchijang.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class JacksonConfig {
    @Bean
    fun jackson2ObjectMapperBuilder(): Jackson2ObjectMapperBuilder =
        Jackson2ObjectMapperBuilder()
            .findModulesViaServiceLoader(true)

    @Bean
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper = builder.build()
}
