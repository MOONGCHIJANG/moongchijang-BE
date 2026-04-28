package com.moongchijang.domain.auth.application.dto.response

data class AccessTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)
