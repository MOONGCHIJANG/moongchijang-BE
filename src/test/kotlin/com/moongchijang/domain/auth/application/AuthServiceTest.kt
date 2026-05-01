package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.KakaoAuthUser
import com.moongchijang.domain.user.application.UserService
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.security.jwt.JwtTokenProvider
import com.moongchijang.support.UserFixture
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime

class AuthServiceTest {

    private val kakaoAuthService: KakaoAuthService = Mockito.mock(KakaoAuthService::class.java)
    private val userService: UserService = Mockito.mock(UserService::class.java)
    private val tokenService: TokenService = Mockito.mock(TokenService::class.java)
    private val jwtTokenProvider: JwtTokenProvider = Mockito.mock(JwtTokenProvider::class.java)

    private val authService = AuthService(
        kakaoAuthService = kakaoAuthService,
        userService = userService,
        tokenService = tokenService,
        jwtTokenProvider = jwtTokenProvider,
    )

    @Test
    fun `카카오 로그인 성공 시 토큰과 사용자 정보를 반환한다`() {
        val now = LocalDateTime.of(2026, 4, 28, 12, 0)
        val user =
            UserFixture.createKakaoUser(id = 1L, providerId = "kakao-1", email = "test@example.com", nickname = "테스트유저")
        setAuditFields(user, now, now)

        Mockito.`when`(kakaoAuthService.getKakaoUser("code-123"))
            .thenReturn(KakaoAuthUser("kakao-1", "test@example.com", "테스트유저"))
        Mockito.`when`(
            userService.findOrCreateKakaoUser("kakao-1", "test@example.com", "테스트유저"),
        ).thenReturn(user to true)
        Mockito.`when`(jwtTokenProvider.generateAccessToken(1L)).thenReturn("access-token")
        Mockito.`when`(tokenService.issueRefreshToken(1L)).thenReturn("refresh-token")
        Mockito.`when`(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L)

        val result = authService.loginWithKakao("code-123")

        Assertions.assertEquals("access-token", result.response.accessToken)
        Assertions.assertEquals("refresh-token", result.refreshToken)
        Assertions.assertTrue(result.response.isNewUser)
        Assertions.assertEquals(1L, result.response.user.id)
        Assertions.assertEquals("테스트유저", result.response.user.nickname)
    }

    @Test
    fun `재발급 요청 시 유효한 리프레시 토큰이면 새 토큰을 반환한다`() {
        val request = Mockito.mock(HttpServletRequest::class.java)

        Mockito.`when`(tokenService.extractRefreshToken(request)).thenReturn("old-refresh")
        Mockito.`when`(tokenService.getUserIdByRefreshToken("old-refresh")).thenReturn(3L)
        Mockito.`when`(tokenService.reissueRefreshToken(3L, "old-refresh")).thenReturn("new-refresh")
        Mockito.`when`(jwtTokenProvider.generateAccessToken(3L)).thenReturn("new-access")
        Mockito.`when`(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L)

        val result = authService.reissueAccessToken(request)

        Assertions.assertEquals("new-refresh", result.refreshToken)
        Assertions.assertEquals("new-access", result.response.accessToken)
        Assertions.assertFalse(result.response.accessToken.isBlank())
    }

    @Test
    fun `재발급 요청 시 리프레시 토큰이 없으면 예외를 던진다`() {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(tokenService.extractRefreshToken(request)).thenReturn(null)

        val exception = assertThrows<CustomException> {
            authService.reissueAccessToken(request)
        }

        Assertions.assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `로그아웃 시 리프레시 토큰 삭제를 호출한다`() {
        authService.logout(9L)
        Mockito.verify(tokenService).deleteByUserId(9L)
    }

    private fun setAuditFields(user: User, createdAt: LocalDateTime, updatedAt: LocalDateTime) {
        val baseClass = user.javaClass.superclass

        val createdAtField = baseClass.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(user, createdAt)

        val updatedAtField = baseClass.getDeclaredField("updatedAt")
        updatedAtField.isAccessible = true
        updatedAtField.set(user, updatedAt)
    }
}