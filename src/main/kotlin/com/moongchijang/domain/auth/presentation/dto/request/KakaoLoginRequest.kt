package com.moongchijang.domain.auth.presentation.dto.request

import jakarta.validation.constraints.NotBlank

data class KakaoLoginRequest(
    @field:NotBlank
    var authorizationCode: String,
    var redirectUri: String? = null,
)
