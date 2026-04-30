package com.moongchijang.application.auth.dto

import com.moongchijang.application.auth.dto.response.AccessTokenResponse

data class AccessTokenReissueResult(

    val response: AccessTokenResponse,
    val refreshToken: String,
)

