package com.moongchijang.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer

@Configuration
class PageableConfig {

    @Bean
    fun pageableCustomizer(): PageableHandlerMethodArgumentResolverCustomizer {
        return PageableHandlerMethodArgumentResolverCustomizer { resolver ->
            resolver.setOneIndexedParameters(true)
            resolver.setFallbackPageable(PageRequest.of(0, 20))
        }
    }
}
