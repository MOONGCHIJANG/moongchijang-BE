package com.moongchijang.domain.auth.presentation

import com.moongchijang.domain.auth.application.AuthService
import com.moongchijang.domain.auth.application.TokenService
import com.moongchijang.domain.auth.application.dto.request.KakaoLoginRequest
import com.moongchijang.domain.auth.application.dto.response.AccessTokenResponse
import com.moongchijang.domain.auth.application.dto.response.AuthLoginResponse
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/kakao")
    fun loginWithKakao(
        @Valid @RequestBody request: KakaoLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<AuthLoginResponse> {
        log.info("[AuthController] 카카오 로그인 요청 수신")
        val result = authService.loginWithKakao(request.authorizationCode)

        tokenService.addRefreshTokenCookie(
            response = response,
            refreshToken = result.refreshToken,
        )
        log.info("[AuthController] 카카오 로그인 응답 완료: userId={}", result.response.user.id)

        return ApiResponse.success(result.response)
    }

    @PostMapping("/refresh")
    fun reissueAccessToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<AccessTokenResponse> {
        log.info("[AuthController] 액세스 토큰 재발급 요청 수신")
        val result = authService.reissueAccessToken(request)

        tokenService.addRefreshTokenCookie(
            response = response,
            refreshToken = result.refreshToken,
        )
        log.info("[AuthController] 액세스 토큰 재발급 응답 완료")

        return ApiResponse.success(result.response)
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
        response: HttpServletResponse,
    ): ApiResponse<Nothing> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        log.info("[AuthController] 로그아웃 요청 수신: userId={}", userId)

        authService.logout(userId)
        tokenService.clearRefreshTokenCookie(response)

        log.info("[AuthController] 로그아웃 응답 완료: userId={}", userId)
        return ApiResponse.success()
    }
}
