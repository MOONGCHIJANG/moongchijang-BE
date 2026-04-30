package com.moongchijang.application.auth

import com.moongchijang.application.auth.dto.AccessTokenReissueResult
import com.moongchijang.application.auth.dto.AuthLoginResult
import com.moongchijang.application.auth.dto.KakaoAuthUser
import com.moongchijang.application.auth.dto.AccessTokenResponse
import com.moongchijang.application.auth.dto.AuthLoginResponse
import com.moongchijang.application.auth.dto.AuthUserResponse
import com.moongchijang.application.user.UserService
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.security.jwt.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val kakaoAuthService: KakaoAuthService,
    private val userService: UserService,
    private val tokenService: TokenService,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun loginWithKakao(authorizationCode: String): AuthLoginResult {
        log.info("[AuthService] 카카오 로그인 처리 시작")
        val kakaoUser: KakaoAuthUser = kakaoAuthService.getKakaoUser(authorizationCode)

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
}
