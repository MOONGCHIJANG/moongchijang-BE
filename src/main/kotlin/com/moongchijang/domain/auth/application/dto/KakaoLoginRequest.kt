package com.moongchijang.domain.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "카카오 로그인 요청")
data class KakaoLoginRequest(

    @field:NotBlank(message = "인가 코드는 필수입니다.")
    @field:Size(max = 500, message = "인가 코드는 500자 이하여야 합니다.")
    @field:Schema(description = "카카오 OAuth 인가 코드", example = "z8Y7x6W5v4U3t2S1")
    var authorizationCode: String,

    @field:Size(max = 500, message = "redirectUri는 500자 이하여야 합니다.")
    @field:Schema(
        description = "인가 코드 발급 시 사용한 Redirect URI (프론트에서 수신한 경우 전달)",
        example = "http://localhost:3000/oauth",
        nullable = true,
    )
    var redirectUri: String? = null,
)
