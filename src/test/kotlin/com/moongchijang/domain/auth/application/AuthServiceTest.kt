package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.KakaoAuthUser
import com.moongchijang.domain.auth.application.dto.EmailLoginRequest
import com.moongchijang.domain.auth.application.dto.EmailSignupRequest
import com.moongchijang.domain.auth.application.dto.KakaoLoginRequest
import com.moongchijang.domain.auth.application.port.EmailSignupTokenStore
import com.moongchijang.domain.user.application.UserService
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.security.crypto.AesGcmPersonalInfoEncryptor
import com.moongchijang.security.crypto.HmacSha256PersonalInfoHasher
import com.moongchijang.security.crypto.PersonalInfoEncryptionProperties
import com.moongchijang.security.crypto.PersonalInfoManager
import com.moongchijang.security.jwt.JwtTokenProvider
import com.moongchijang.support.UserFixture
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

class AuthServiceTest {

    private val kakaoAuthService: KakaoAuthService = Mockito.mock(KakaoAuthService::class.java)
    private val userService: UserService = Mockito.mock(UserService::class.java)
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val emailSignupTokenStore: EmailSignupTokenStore = Mockito.mock(EmailSignupTokenStore::class.java)
    private val tokenService: TokenService = Mockito.mock(TokenService::class.java)
    private val jwtTokenProvider: JwtTokenProvider = Mockito.mock(JwtTokenProvider::class.java)
    private val passwordEncoder: PasswordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val authMetricsRecorder: AuthMetricsRecorder = Mockito.mock(AuthMetricsRecorder::class.java)
    private val personalInfoProperties = PersonalInfoEncryptionProperties(
        secretKey = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    )
    private val personalInfoManager = PersonalInfoManager(
        AesGcmPersonalInfoEncryptor(personalInfoProperties),
        HmacSha256PersonalInfoHasher(personalInfoProperties),
    )

    private val authService = AuthService(
        kakaoAuthService = kakaoAuthService,
        userService = userService,
        userRepository = userRepository,
        emailSignupTokenStore = emailSignupTokenStore,
        tokenService = tokenService,
        jwtTokenProvider = jwtTokenProvider,
        passwordEncoder = passwordEncoder,
        authMetricsRecorder = authMetricsRecorder,
        personalInfoManager = personalInfoManager,
    )

    @Test
    fun `카카오 로그인 성공 시 토큰과 사용자 정보 반환`() {
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

        val result = authService.loginWithKakao(
            KakaoLoginRequest(
                authorizationCode = "code-123",
                redirectUri = null,
            )
        )

        Assertions.assertEquals("access-token", result.response.accessToken)
        Assertions.assertEquals("refresh-token", result.refreshToken)
        Assertions.assertTrue(result.response.isNewUser)
        Assertions.assertEquals(1L, result.response.user.id)
        Assertions.assertEquals("테스트유저", result.response.user.nickname)
    }

    @Test
    fun `재발급 요청 시 유효한 리프레시 토큰 기반 새 토큰 반환`() {
        val request = Mockito.mock(HttpServletRequest::class.java)
        val user = UserFixture.createKakaoUser(id = 3L, providerId = "kakao-3")

        Mockito.`when`(tokenService.extractRefreshToken(request)).thenReturn("old-refresh")
        Mockito.`when`(tokenService.getUserIdByRefreshToken("old-refresh")).thenReturn(3L)
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(user)
        Mockito.`when`(tokenService.reissueRefreshToken(3L, "old-refresh")).thenReturn("new-refresh")
        Mockito.`when`(jwtTokenProvider.generateAccessToken(3L)).thenReturn("new-access")
        Mockito.`when`(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L)

        val result = authService.reissueAccessToken(request)

        Assertions.assertEquals("new-refresh", result.refreshToken)
        Assertions.assertEquals("new-access", result.response.accessToken)
        Assertions.assertFalse(result.response.accessToken.isBlank())
    }

    @Test
    fun `재발급 요청 시 리프레시 토큰 누락 예외`() {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(tokenService.extractRefreshToken(request)).thenReturn(null)

        val exception = assertThrows<CustomException> {
            authService.reissueAccessToken(request)
        }

        Assertions.assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `재발급 요청 시 탈퇴 사용자는 토큰 삭제 후 예외`() {
        val request = Mockito.mock(HttpServletRequest::class.java)

        Mockito.`when`(tokenService.extractRefreshToken(request)).thenReturn("deleted-user-refresh")
        Mockito.`when`(tokenService.getUserIdByRefreshToken("deleted-user-refresh")).thenReturn(7L)
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(null)

        val exception = assertThrows<CustomException> {
            authService.reissueAccessToken(request)
        }

        Assertions.assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.errorCode)
        Mockito.verify(tokenService).deleteByUserId(7L)
        Mockito.verify(tokenService, Mockito.never()).reissueRefreshToken(Mockito.anyLong(), Mockito.anyString())
    }

    @Test
    fun `로그아웃 시 리프레시 토큰 삭제 호출`() {
        authService.logout(9L, UserRole.SELLER)
        Mockito.verify(userService).saveLastRole(9L, UserRole.SELLER)
        Mockito.verify(tokenService).deleteByUserId(9L)
    }

    @Test
    fun `이메일 회원가입 성공 시 사용자 생성 및 토큰 반환`() {
        val now = LocalDateTime.of(2026, 5, 11, 12, 0)
        val user = User(
            id = 11L,
            provider = AuthProvider.EMAIL,
            providerId = null,
            email = "new@example.com",
            passwordHash = "hashed-password",
            nickname = null,
            phoneNumber = null,
            signupCompleted = false,
        )
        setAuditFields(user, now, now)

        Mockito.`when`(emailSignupTokenStore.isValid("new@example.com", "signup-token")).thenReturn(true)
        Mockito.`when`(passwordEncoder.encode("abc12345")).thenReturn("hashed-password")
        Mockito.`when`(userService.createEmailUser("new@example.com", "hashed-password")).thenReturn(user)
        Mockito.`when`(jwtTokenProvider.generateAccessToken(11L)).thenReturn("email-access-token")
        Mockito.`when`(tokenService.issueRefreshToken(11L)).thenReturn("email-refresh-token")
        Mockito.`when`(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L)

        val result = authService.signupWithEmail(
            EmailSignupRequest(
                email = "new@example.com",
                password = "abc12345",
                signupToken = "signup-token",
            )
        )

        Assertions.assertEquals("email-access-token", result.response.accessToken)
        Assertions.assertEquals("email-refresh-token", result.refreshToken)
        Assertions.assertTrue(result.response.isNewUser)
        Assertions.assertEquals(11L, result.response.user.id)
        Mockito.verify(emailSignupTokenStore).delete("new@example.com")
    }

    @Test
    fun `이메일 회원가입 시 signupToken 불일치면 예외`() {
        Mockito.`when`(emailSignupTokenStore.isValid("new@example.com", "bad-token")).thenReturn(false)

        val exception = assertThrows<CustomException> {
            authService.signupWithEmail(
                EmailSignupRequest(
                    email = "new@example.com",
                    password = "abc12345",
                    signupToken = "bad-token",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.INVALID_SIGNUP_TOKEN, exception.errorCode)
    }

    @Test
    fun `이메일 로그인 성공 시 토큰 반환`() {
        val now = LocalDateTime.of(2026, 5, 11, 12, 0)
        val user = User(
            id = 22L,
            provider = AuthProvider.EMAIL,
            providerId = null,
            email = "login@example.com",
            passwordHash = "hashed-password",
            nickname = "테스트유저",
            phoneNumber = null,
            signupCompleted = true,
        )
        setAuditFields(user, now, now)

        Mockito.`when`(userService.findActiveEmailUser("login@example.com")).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("abc12345", "hashed-password")).thenReturn(true)
        Mockito.`when`(jwtTokenProvider.generateAccessToken(22L)).thenReturn("login-access-token")
        Mockito.`when`(tokenService.issueRefreshToken(22L)).thenReturn("login-refresh-token")
        Mockito.`when`(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L)

        val result = authService.loginWithEmail(
            EmailLoginRequest(
                email = "login@example.com",
                password = "abc12345",
            )
        )

        Assertions.assertEquals("login-access-token", result.response.accessToken)
        Assertions.assertEquals("login-refresh-token", result.refreshToken)
        Assertions.assertFalse(result.response.isNewUser)
        Assertions.assertEquals(22L, result.response.user.id)
    }

    @Test
    fun `이메일 로그인 시 비밀번호 불일치면 예외`() {
        val user = User(
            id = 33L,
            provider = AuthProvider.EMAIL,
            providerId = null,
            email = "login@example.com",
            passwordHash = "hashed-password",
            nickname = null,
            phoneNumber = null,
            signupCompleted = false,
        )

        Mockito.`when`(userService.findActiveEmailUser("login@example.com")).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("wrong1234", "hashed-password")).thenReturn(false)

        val exception = assertThrows<CustomException> {
            authService.loginWithEmail(
                EmailLoginRequest(
                    email = "login@example.com",
                    password = "wrong1234",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
    }

    @Test
    fun `관리자 이메일 로그인 성공 시 활성 역할을 ADMIN으로 변경하고 토큰을 반환`() {
        val now = LocalDateTime.of(2026, 6, 18, 12, 0)
        val user = UserFixture.createEmailUser(
            id = 55L,
            email = "admin@example.com",
            passwordHash = "hashed-password",
            nickname = "관리자",
        )
        user.grantRole(UserRole.BUYER)
        user.grantRole(UserRole.ADMIN)
        setAuditFields(user, now, now)

        Mockito.`when`(userService.findActiveEmailUser("admin@example.com")).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("abc12345", "hashed-password")).thenReturn(true)
        Mockito.`when`(jwtTokenProvider.generateAccessToken(55L)).thenReturn("admin-access-token")
        Mockito.`when`(tokenService.issueRefreshToken(55L)).thenReturn("admin-refresh-token")
        Mockito.`when`(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L)

        val result = authService.loginAdminWithEmail(
            EmailLoginRequest(
                email = "admin@example.com",
                password = "abc12345",
            )
        )

        Assertions.assertEquals("admin-access-token", result.response.accessToken)
        Assertions.assertEquals("admin-refresh-token", result.refreshToken)
        Assertions.assertEquals(UserRole.ADMIN, result.response.user.role)
        Assertions.assertEquals(UserRole.ADMIN, user.role)
        Assertions.assertEquals(UserRole.ADMIN, user.lastRole)
    }

    @Test
    fun `관리자 이메일 로그인 시 ADMIN 권한이 없으면 예외`() {
        val user = UserFixture.createEmailUser(
            id = 56L,
            email = "buyer@example.com",
            passwordHash = "hashed-password",
        )
        user.grantRole(UserRole.BUYER)

        Mockito.`when`(userService.findActiveEmailUser("buyer@example.com")).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("abc12345", "hashed-password")).thenReturn(true)

        val exception = assertThrows<CustomException> {
            authService.loginAdminWithEmail(
                EmailLoginRequest(
                    email = "buyer@example.com",
                    password = "abc12345",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `이메일 회원가입 시 비밀번호 형식 불일치면 예외`() {
        Mockito.`when`(emailSignupTokenStore.isValid("new@example.com", "signup-token")).thenReturn(true)

        val exception = assertThrows<CustomException> {
            authService.signupWithEmail(
                EmailSignupRequest(
                    email = "new@example.com",
                    password = "password-only",
                    signupToken = "signup-token",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.INVALID_PASSWORD_FORMAT, exception.errorCode)
    }

    @Test
    fun `이메일 회원가입 시 이메일 아이디와 동일한 비밀번호면 예외`() {
        Mockito.`when`(emailSignupTokenStore.isValid("new12345@example.com", "signup-token")).thenReturn(true)

        val exception = assertThrows<CustomException> {
            authService.signupWithEmail(
                EmailSignupRequest(
                    email = "new12345@example.com",
                    password = "new12345",
                    signupToken = "signup-token",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.INVALID_PASSWORD_SAME_AS_EMAIL_ID, exception.errorCode)
    }

    @Test
    fun `이메일 회원가입 시 중복 이메일이면 예외`() {
        Mockito.`when`(emailSignupTokenStore.isValid("dup@example.com", "signup-token")).thenReturn(true)
        Mockito.`when`(passwordEncoder.encode("abc12345")).thenReturn("hashed-password")
        Mockito.`when`(userService.createEmailUser("dup@example.com", "hashed-password"))
            .thenThrow(CustomException(ErrorCode.DUPLICATE_EMAIL))

        val exception = assertThrows<CustomException> {
            authService.signupWithEmail(
                EmailSignupRequest(
                    email = "dup@example.com",
                    password = "abc12345",
                    signupToken = "signup-token",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.errorCode)
    }

    @Test
    fun `이메일 로그인 시 사용자 미존재면 예외`() {
        Mockito.`when`(userService.findActiveEmailUser("none@example.com")).thenReturn(null)

        val exception = assertThrows<CustomException> {
            authService.loginWithEmail(
                EmailLoginRequest(
                    email = "none@example.com",
                    password = "abc12345",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
    }

    @Test
    fun `이메일 로그인 시 비밀번호 해시가 없으면 예외`() {
        val user = User(
            id = 44L,
            provider = AuthProvider.EMAIL,
            providerId = null,
            email = "login@example.com",
            passwordHash = null,
            nickname = null,
            phoneNumber = null,
            signupCompleted = false,
        )
        Mockito.`when`(userService.findActiveEmailUser("login@example.com")).thenReturn(user)

        val exception = assertThrows<CustomException> {
            authService.loginWithEmail(
                EmailLoginRequest(
                    email = "login@example.com",
                    password = "abc12345",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
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
