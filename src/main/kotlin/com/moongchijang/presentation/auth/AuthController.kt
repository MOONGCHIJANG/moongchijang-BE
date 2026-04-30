package com.moongchijang.presentation.auth

import com.moongchijang.application.auth.AuthService
import com.moongchijang.application.auth.TokenService
import com.moongchijang.application.auth.dto.KakaoLoginRequest
import com.moongchijang.application.auth.dto.AccessTokenResponse
import com.moongchijang.application.auth.dto.AuthLoginResponse
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Auth", description = "인증 API")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/kakao")
    @Operation(summary = "카카오 로그인", description = "인가 코드를 받아 로그인/회원가입 분기 후 액세스 토큰을 발급합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
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
    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰 쿠키를 검증해 액세스 토큰을 재발급합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "재발급 성공"),
            SwaggerApiResponse(responseCode = "401", description = "리프레시 토큰 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
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
    @Operation(summary = "로그아웃", description = "리프레시 토큰을 폐기하고 쿠키를 제거합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "로그아웃 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
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
