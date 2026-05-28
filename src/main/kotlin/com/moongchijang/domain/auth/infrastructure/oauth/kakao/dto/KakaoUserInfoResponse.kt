package com.moongchijang.domain.auth.infrastructure.oauth.kakao.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoUserInfoResponse(
    val id: Long? = null,

    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount? = null,
) {
    data class KakaoAccount(
        val profile: KakaoProfile? = null,
        val email: String? = null,
    )

    data class KakaoProfile(
        val nickname: String? = null,
    )
}
