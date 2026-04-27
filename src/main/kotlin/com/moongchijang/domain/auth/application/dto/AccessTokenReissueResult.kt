package com.moongchijang.domain.auth.application.dto

import com.moongchijang.domain.auth.application.dto.response.AccessTokenResponse

data class AccessTokenReissueResult(
    val response: AccessTokenResponse,
    val refreshToken: String,
)

