package com.moongchijang.application.auth.dto

data class AuthLoginResult(

    val response: AuthLoginResponse,
    val refreshToken: String,
)
