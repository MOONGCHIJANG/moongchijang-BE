package com.moongchijang.application.auth.dto

import com.moongchijang.application.auth.dto.response.AuthLoginResponse

data class AuthLoginResult(

    val response: AuthLoginResponse,
    val refreshToken: String,
)
