package com.moongchijang.domain.auth.presentation

import com.moongchijang.domain.auth.application.AuthService
import com.moongchijang.domain.auth.application.TokenService
import com.moongchijang.domain.auth.application.dto.request.KakaoLoginRequest
import com.moongchijang.domain.auth.application.dto.response.AuthLoginResponse
import com.moongchijang.global.response.ApiResponse
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
) {

    @PostMapping("/kakao")
    fun loginWithKakao(
        @Valid @RequestBody request: KakaoLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<AuthLoginResponse> {
        val result = authService.loginWithKakao(request.authorizationCode)

        tokenService.addRefreshTokenCookie(
            response = response,
            refreshToken = result.refreshToken,
        )

        return ApiResponse.success(result.response)
    }
}
