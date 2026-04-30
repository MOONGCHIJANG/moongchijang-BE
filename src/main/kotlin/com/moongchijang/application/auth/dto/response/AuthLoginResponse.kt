package com.moongchijang.application.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 응답")
data class AuthLoginResponse(

    @field:Schema(description = "서비스 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,

    @field:Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String = "Bearer",

    @field:Schema(description = "액세스 토큰 만료까지 남은 시간(초)", example = "3600")
    val expiresIn: Long,

    @field:Schema(description = "신규 사용자 여부 (true면 추가정보 입력 필요)", example = "true")
    val isNewUser: Boolean,

    @field:Schema(description = "로그인한 사용자 정보")
    val user: AuthUserResponse,
)
