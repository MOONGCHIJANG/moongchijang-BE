package com.moongchijang.application.auth.dto

data class AccessTokenReissueResult(

    val response: AccessTokenResponse,
    val refreshToken: String,
)
