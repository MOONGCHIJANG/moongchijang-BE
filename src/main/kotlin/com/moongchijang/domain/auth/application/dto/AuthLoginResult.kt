package com.moongchijang.domain.auth.application.dto

data class AuthLoginResult(

    val response: AuthLoginResponse,
    val refreshToken: String,
)
