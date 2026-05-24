package com.moongchijang.domain.auth.presentation

import com.moongchijang.domain.auth.application.AuthService
import com.moongchijang.domain.auth.application.TokenService
import com.moongchijang.domain.auth.application.dto.KakaoLoginRequest
import com.moongchijang.domain.auth.application.dto.EmailSignupRequest
import com.moongchijang.domain.auth.application.dto.EmailLoginRequest
import com.moongchijang.domain.auth.application.dto.AccessTokenResponse
import com.moongchijang.domain.auth.application.dto.AuthLoginResponse
import com.moongchijang.domain.user.application.UserService
import com.moongchijang.domain.user.application.dto.EmailAvailabilityResponse
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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 API")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
    private val userService: UserService,
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
        val (authLoginResponse, refreshToken) = authService.loginWithKakao(request)

        tokenService.addRefreshTokenCookie(
            response = response,
            refreshToken = refreshToken,
        )
        log.info("[AuthController] 카카오 로그인 응답 완료: userId={}", authLoginResponse.user.id)

        return ApiResponse.success(authLoginResponse)
    }

    @PostMapping("/email/signup")
    @Operation(summary = "이메일 회원가입", description = "이메일 인증 완료 후 비밀번호를 설정해 회원가입하고 로그인 처리합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "회원가입 성공"),
            SwaggerApiResponse(responseCode = "400", description = "요청값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "회원가입 인증정보 유효성 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "409", description = "이미 가입된 이메일", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun signupWithEmail(
        @Valid @RequestBody request: EmailSignupRequest,
        response: HttpServletResponse,
    ): ApiResponse<AuthLoginResponse> {
        log.info("[AuthController] 이메일 회원가입 요청 수신")
        val (authLoginResponse, refreshToken) = authService.signupWithEmail(request)

        tokenService.addRefreshTokenCookie(
            response = response,
            refreshToken = refreshToken,
        )
        log.info("[AuthController] 이메일 회원가입 응답 완료: userId={}", authLoginResponse.user.id)

        return ApiResponse.success(authLoginResponse)
    }

    @PostMapping("/email/login")
    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호를 검증해 로그인 처리합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
            SwaggerApiResponse(responseCode = "400", description = "요청값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun loginWithEmail(
        @Valid @RequestBody request: EmailLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<AuthLoginResponse> {
        log.info("[AuthController] 이메일 로그인 요청 수신")
        val (authLoginResponse, refreshToken) = authService.loginWithEmail(request)

        tokenService.addRefreshTokenCookie(
            response = response,
            refreshToken = refreshToken,
        )
        log.info("[AuthController] 이메일 로그인 응답 완료: userId={}", authLoginResponse.user.id)

        return ApiResponse.success(authLoginResponse)
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
        val (accessTokenResponse, refreshToken) = authService.reissueAccessToken(request)

        tokenService.addRefreshTokenCookie(
            response = response,
            refreshToken = refreshToken,
        )
        log.info("[AuthController] 액세스 토큰 재발급 응답 완료")

        return ApiResponse.success(accessTokenResponse)
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "로그아웃", description = "리프레시 토큰을 폐기하고 쿠키를 제거합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "로그아웃 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun logout(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        response: HttpServletResponse,
    ): ApiResponse<Nothing> {
        val userId = principal.id
        log.info("[AuthController] 로그아웃 요청 수신: userId={}", userId)

        authService.logout(userId, principal.role)
        tokenService.clearRefreshTokenCookie(response)

        log.info("[AuthController] 로그아웃 응답 완료: userId={}", userId)
        return ApiResponse.success()
    }

    @GetMapping("/email/availability")
    @Operation(summary = "이메일 중복 확인", description = "이메일 회원가입 전 가입 가능 이메일인지 확인합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "이메일 사용 가능 여부 반환"),
            SwaggerApiResponse(responseCode = "400", description = "이메일 형식 오류", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun checkEmailAvailability(
        @RequestParam email: String,
    ): ApiResponse<EmailAvailabilityResponse> {
        log.info("[AuthController] 이메일 중복 확인 요청 수신")
        val response = userService.checkEmailAvailability(email)
        log.info("[AuthController] 이메일 중복 확인 응답 완료")
        return ApiResponse.success(response)
    }
}
