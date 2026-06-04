package com.moongchijang.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("뭉치장 API")
                    .description(
                        """
                        공동구매 플랫폼 뭉치장의 API 명세입니다.
                        
                        - 저장 시각(createdAt, updatedAt, 상태 전이 시각 등)은 UTC 기준으로 관리합니다.
                        - 비즈니스 계산(D-day, 마감 여부, 픽업 오늘/내일 판단 등)은 KST(Asia/Seoul) 기준으로 처리합니다.
                        - `date-time` 응답은 현재 LocalDateTime 직렬화 형식인 `yyyy-MM-dd'T'HH:mm:ss` 문자열을 사용합니다.
                        """.trimIndent()
                    )
                    .version("v1")
            )
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components().addSecuritySchemes(
                    securitySchemeName,
                    SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
    }
}
