package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.AuthLoginResult
import com.moongchijang.domain.auth.application.dto.KakaoAuthUser
import com.moongchijang.domain.auth.application.dto.response.AuthLoginResponse
import com.moongchijang.domain.auth.application.dto.response.AuthUserResponse
import com.moongchijang.domain.user.application.UserService
import com.moongchijang.security.jwt.JwtTokenProvider
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val kakaoAuthService: KakaoAuthService,
    private val userService: UserService,
    private val tokenService: TokenService,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    @Transactional
    fun loginWithKakao(authorizationCode: String): AuthLoginResult {
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

        return AuthLoginResult(
            response = response,
            refreshToken = refreshToken,
        )
    }
}
