package com.moongchijang.domain.auth.application.dto

data class KakaoAuthUser(

    val providerId: String,
    val email: String,
    val nickname: String,
)
