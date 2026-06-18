package com.moongchijang.domain.auth.presentation

import com.moongchijang.domain.auth.application.AuthService
import com.moongchijang.domain.auth.application.TokenService
import com.moongchijang.domain.auth.application.dto.AuthLoginResponse
import com.moongchijang.domain.auth.application.dto.EmailLoginRequest
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/api/v1/auth/admin")
@Tag(name = "Admin Auth", description = "관리자 인증 API")
class AdminAuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/email/login")
    @Operation(summary = "관리자 이메일 로그인", description = "이메일과 비밀번호를 검증하고 ADMIN 권한 보유 사용자만 로그인 처리합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
            SwaggerApiResponse(responseCode = "400", description = "요청값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "관리자 권한 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun loginAdminWithEmail(
        @Valid @RequestBody request: EmailLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<AuthLoginResponse> {
        log.info("[AdminAuthController] 관리자 이메일 로그인 요청 수신")
        val (authLoginResponse, refreshToken) = authService.loginAdminWithEmail(request)

        tokenService.addRefreshTokenCookie(
            response = response,
            refreshToken = refreshToken,
        )
        log.info("[AdminAuthController] 관리자 이메일 로그인 응답 완료: userId={}", authLoginResponse.user.id)

        return ApiResponse.success(authLoginResponse)
    }
}
