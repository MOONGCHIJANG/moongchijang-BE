package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.AccessTokenReissueResult
import com.moongchijang.domain.auth.application.dto.AccessTokenResponse
import com.moongchijang.domain.auth.application.dto.AuthLoginResult
import com.moongchijang.domain.auth.application.dto.AuthLoginResponse
import com.moongchijang.domain.auth.application.dto.AuthUserResponse
import com.moongchijang.domain.auth.application.dto.EmailLoginRequest
import com.moongchijang.domain.auth.application.dto.EmailSignupRequest
import com.moongchijang.domain.auth.application.dto.KakaoAuthUser
import com.moongchijang.domain.auth.application.dto.KakaoLoginRequest
import com.moongchijang.domain.auth.application.port.EmailSignupTokenStore
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.application.UserService
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.MaskingUtils.maskEmail
import com.moongchijang.security.jwt.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val kakaoAuthService: KakaoAuthService,
    private val userService: UserService,
    private val emailSignupTokenStore: EmailSignupTokenStore,
    private val tokenService: TokenService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun loginWithKakao(request: KakaoLoginRequest): AuthLoginResult {
        log.info("[AuthService] 카카오 로그인 처리 시작")
        val kakaoUser: KakaoAuthUser = kakaoAuthService.getKakaoUser(request.authorizationCode)

        val (user, isNewUser) = userService.findOrCreateKakaoUser(
            providerId = kakaoUser.providerId,
            email = kakaoUser.email,
            nickname = kakaoUser.nickname,
        )

        val accessToken = jwtTokenProvider.generateAccessToken(user.id!!)
        val refreshToken = tokenService.issueRefreshToken(user.id!!)
        val expiresIn = jwtTokenProvider.getAccessTokenExpiresInSeconds()

        val response = AuthLoginResponse(
            accessToken = accessToken,
            tokenType = "Bearer",
            expiresIn = expiresIn,
            isNewUser = isNewUser,
            user = AuthUserResponse.from(user),
        )

        log.info(
            "[AuthService] 카카오 로그인 처리 완료: userId={}, isNewUser={}",
            user.id,
            isNewUser,
        )

        return AuthLoginResult(
            response = response,
            refreshToken = refreshToken,
        )
    }

    @Transactional
    fun reissueAccessToken(request: HttpServletRequest): AccessTokenReissueResult {
        log.info("[AuthService] 액세스 토큰 재발급 처리 시작")

        val refreshToken = tokenService.extractRefreshToken(request)
            ?: throw CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)

        val userId = tokenService.getUserIdByRefreshToken(refreshToken)
            ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        val newRefreshToken = tokenService.reissueRefreshToken(userId, refreshToken)
        val accessToken = jwtTokenProvider.generateAccessToken(userId)
        val expiresIn = jwtTokenProvider.getAccessTokenExpiresInSeconds()

        val response = AccessTokenResponse(
            accessToken = accessToken,
            tokenType = "Bearer",
            expiresIn = expiresIn,
        )

        log.info("[AuthService] 액세스 토큰 재발급 처리 완료: userId={}", userId)

        return AccessTokenReissueResult(
            response = response,
            refreshToken = newRefreshToken,
        )
    }

    @Transactional
    fun logout(userId: Long) {
        log.info("[AuthService] 로그아웃 처리 시작: userId={}", userId)
        tokenService.deleteByUserId(userId)
        log.info("[AuthService] 로그아웃 처리 완료: userId={}", userId)
    }

    private fun validateSignupTokenForNormalizedEmail(email: String, signupToken: String) {
        if (!emailSignupTokenStore.isValid(email, signupToken)) {
            throw CustomException(ErrorCode.INVALID_SIGNUP_TOKEN)
        }
    }

    @Transactional
    fun signupWithEmail(request: EmailSignupRequest): AuthLoginResult {
        val normalizedEmail = request.email.trim().lowercase()
        log.info("[AuthService] 이메일 회원가입 처리 시작: email={}", normalizedEmail)

        validateSignupTokenForNormalizedEmail(normalizedEmail, request.signupToken)
        validatePasswordPolicy(normalizedEmail, request.password)

        val passwordHash = requireNotNull(passwordEncoder.encode(request.password)) {
            "Password hash must not be null"
        }
        val user: User = userService.createEmailUser(normalizedEmail, passwordHash)

        emailSignupTokenStore.delete(normalizedEmail)

        val accessToken = jwtTokenProvider.generateAccessToken(user.id!!)
        val refreshToken = tokenService.issueRefreshToken(user.id!!)
        val expiresIn = jwtTokenProvider.getAccessTokenExpiresInSeconds()

        val response = AuthLoginResponse(
            accessToken = accessToken,
            tokenType = "Bearer",
            expiresIn = expiresIn,
            isNewUser = true,
            user = AuthUserResponse.from(user),
        )

        log.info("[AuthService] 이메일 회원가입 처리 완료: userId={}", user.id)
        return AuthLoginResult(
            response = response,
            refreshToken = refreshToken,
        )
    }

    private fun validatePasswordPolicy(email: String, password: String) {
        if (!PASSWORD_REGEX.matches(password)) {
            throw CustomException(ErrorCode.INVALID_PASSWORD_FORMAT)
        }

        val emailLocalPart = email.substringBefore("@")
        if (emailLocalPart.equals(password, ignoreCase = true)) {
            throw CustomException(ErrorCode.INVALID_PASSWORD_SAME_AS_EMAIL_ID)
        }
    }

    @Transactional
    fun loginWithEmail(request: EmailLoginRequest): AuthLoginResult {
        val normalizedEmail = request.email.trim().lowercase()
        log.info("[AuthService] 이메일 로그인 처리 시작: email={}", maskEmail(normalizedEmail))

        val user = userService.findActiveEmailUser(normalizedEmail)
            ?: throw CustomException(ErrorCode.INVALID_CREDENTIALS)

        val passwordHash = user.passwordHash ?: throw CustomException(ErrorCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(request.password, passwordHash)) {
            throw CustomException(ErrorCode.INVALID_CREDENTIALS)
        }

        val accessToken = jwtTokenProvider.generateAccessToken(user.id!!)
        val refreshToken = tokenService.issueRefreshToken(user.id!!)
        val expiresIn = jwtTokenProvider.getAccessTokenExpiresInSeconds()

        val response = AuthLoginResponse(
            accessToken = accessToken,
            tokenType = "Bearer",
            expiresIn = expiresIn,
            isNewUser = false,
            user = AuthUserResponse.from(user),
        )

        log.info("[AuthService] 이메일 로그인 처리 완료: userId={}", user.id)

        return AuthLoginResult(
            response = response,
            refreshToken = refreshToken,
        )
    }

    companion object {
        private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$")
    }
}
