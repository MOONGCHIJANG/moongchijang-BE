package com.moongchijang.domain.auth.application.dto

data class AccessTokenReissueResult(

    val response: AccessTokenResponse,
    val refreshToken: String,
)
