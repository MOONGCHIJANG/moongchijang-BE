package com.moongchijang.domain.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "액세스 토큰 재발급 응답")
data class AccessTokenResponse(

    @field:Schema(description = "재발급된 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,

    @field:Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String = "Bearer",

    @field:Schema(description = "액세스 토큰 만료까지 남은 시간(초)", example = "3600")
    val expiresIn: Long,
)
