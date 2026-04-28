package com.moongchijang.domain.auth.application.dto

import com.moongchijang.domain.auth.application.dto.response.AuthLoginResponse

data class AuthLoginResult(
    val response: AuthLoginResponse,
    val refreshToken: String,
)
