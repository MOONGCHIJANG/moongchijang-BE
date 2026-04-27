package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.AuthLoginResult
import com.moongchijang.domain.auth.application.dto.KakaoAuthUser
import com.moongchijang.domain.auth.application.dto.response.AuthLoginResponse
import com.moongchijang.domain.auth.application.dto.response.AuthUserResponse
import com.moongchijang.domain.user.application.UserService
import com.moongchijang.security.jwt.JwtTokenProvider
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
}
