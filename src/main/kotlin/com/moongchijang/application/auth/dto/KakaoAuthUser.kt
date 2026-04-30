package com.moongchijang.application.auth.dto

data class KakaoAuthUser(

    val providerId: String,
    val email: String,
    val nickname: String,
)
