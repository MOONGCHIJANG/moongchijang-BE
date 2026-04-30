package com.moongchijang.infrastructure.oauth.kakao.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoTokenResponse(
    @JsonProperty("token_type")
    val tokenType: String? = null,

    @JsonProperty("access_token")
    val accessToken: String? = null,

    @JsonProperty("expires_in")
    val expiresIn: Int? = null,

    @JsonProperty("refresh_token")
    val refreshToken: String? = null,

    @JsonProperty("refresh_token_expires_in")
    val refreshTokenExpiresIn: Int? = null,

    val scope: String? = null,
)
