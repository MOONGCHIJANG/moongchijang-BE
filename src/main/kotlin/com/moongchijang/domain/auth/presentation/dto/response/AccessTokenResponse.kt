package com.moongchijang.domain.auth.presentation.dto.response

data class AccessTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)
