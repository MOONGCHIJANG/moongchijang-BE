package com.moongchijang.application.auth.dto.response

data class AuthLoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val isNewUser: Boolean,
    val user: AuthUserResponse,
)
