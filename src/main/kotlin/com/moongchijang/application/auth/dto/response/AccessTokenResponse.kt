package com.moongchijang.application.auth.dto.response

data class AccessTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)
