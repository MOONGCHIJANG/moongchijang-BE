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
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.application.UserService
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.MaskingUtils.maskEmail
import com.moongchijang.security.crypto.PersonalInfoManager
import com.moongchijang.security.jwt.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class AuthService(
    private val kakaoAuthService: KakaoAuthService,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val emailSignupTokenStore: EmailSignupTokenStore,
    private val tokenService: TokenService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val authMetricsRecorder: AuthMetricsRecorder,
    private val personalInfoManager: PersonalInfoManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun loginWithKakao(request: KakaoLoginRequest): AuthLoginResult {
        return runCatching {
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
                user = toAuthUserResponse(user),
            )

            log.info(
                "[AuthService] 카카오 로그인 처리 완료: userId={}, isNewUser={}",
                user.id,
                isNewUser,
            )
            recordAfterCommit { authMetricsRecorder.recordLogin(provider = "kakao", result = "success") }

            AuthLoginResult(
                response = response,
                refreshToken = refreshToken,
            )
        }.getOrElse { throwable ->
            authMetricsRecorder.recordLogin(provider = "kakao", result = "failure")
            throw throwable
        }
    }

    @Transactional
    fun reissueAccessToken(request: HttpServletRequest): AccessTokenReissueResult {
        return runCatching {
            log.info("[AuthService] 액세스 토큰 재발급 처리 시작")

            val refreshToken = tokenService.extractRefreshToken(request)
                ?: throw CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)

            val userId = tokenService.getUserIdByRefreshToken(refreshToken)
                ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

            val activeUser = userRepository.findByIdAndDeletedAtIsNull(userId)
            if (activeUser == null) {
                tokenService.deleteByUserId(userId)
                throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)
            }

            val newRefreshToken = tokenService.reissueRefreshToken(userId, refreshToken)
            val accessToken = jwtTokenProvider.generateAccessToken(userId)
            val expiresIn = jwtTokenProvider.getAccessTokenExpiresInSeconds()

            val response = AccessTokenResponse(
                accessToken = accessToken,
                tokenType = "Bearer",
                expiresIn = expiresIn,
            )

            log.info("[AuthService] 액세스 토큰 재발급 처리 완료: userId={}", userId)
            recordAfterCommit { authMetricsRecorder.recordTokenReissue(result = "success") }

            AccessTokenReissueResult(
                response = response,
                refreshToken = newRefreshToken,
            )
        }.getOrElse { throwable ->
            authMetricsRecorder.recordTokenReissue(result = "failure")
            throw throwable
        }
    }

    @Transactional
    fun logout(userId: Long, role: UserRole) {
        log.info("[AuthService] 로그아웃 처리 시작: userId={}", userId)
        userService.saveLastRole(userId, role)
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
        return runCatching {
            val normalizedEmail = request.email.trim().lowercase()
            log.info("[AuthService] 이메일 회원가입 처리 시작: email={}", maskEmail(normalizedEmail))

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
                user = toAuthUserResponse(user),
            )

            log.info("[AuthService] 이메일 회원가입 처리 완료: userId={}", user.id)
            recordAfterCommit { authMetricsRecorder.recordSignup(method = "email", result = "success") }

            AuthLoginResult(
                response = response,
                refreshToken = refreshToken,
            )
        }.getOrElse { throwable ->
            authMetricsRecorder.recordSignup(method = "email", result = "failure")
            throw throwable
        }
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
        return runCatching {
            val normalizedEmail = request.email.trim().lowercase()
            log.info("[AuthService] 이메일 로그인 처리 시작: email={}", maskEmail(normalizedEmail))

            val user = authenticateEmailUser(normalizedEmail, request.password)

            val accessToken = jwtTokenProvider.generateAccessToken(user.id!!)
            val refreshToken = tokenService.issueRefreshToken(user.id!!)
            val expiresIn = jwtTokenProvider.getAccessTokenExpiresInSeconds()

            val response = AuthLoginResponse(
                accessToken = accessToken,
                tokenType = "Bearer",
                expiresIn = expiresIn,
                isNewUser = false,
                user = toAuthUserResponse(user),
            )

            log.info("[AuthService] 이메일 로그인 처리 완료: userId={}", user.id)
            recordAfterCommit { authMetricsRecorder.recordLogin(provider = "email", result = "success") }

            AuthLoginResult(
                response = response,
                refreshToken = refreshToken,
            )
        }.getOrElse { throwable ->
            authMetricsRecorder.recordLogin(provider = "email", result = "failure")
            throw throwable
        }
    }

    @Transactional
    fun loginAdminWithEmail(request: EmailLoginRequest): AuthLoginResult {
        return runCatching {
            val normalizedEmail = request.email.trim().lowercase()
            log.info("[AuthService] 관리자 이메일 로그인 처리 시작: email={}", maskEmail(normalizedEmail))

            val user = authenticateEmailUser(normalizedEmail, request.password)
            if (!user.hasRole(UserRole.ADMIN)) {
                throw CustomException(ErrorCode.FORBIDDEN)
            }

            user.role = UserRole.ADMIN
            user.saveLastRole(UserRole.ADMIN)

            val accessToken = jwtTokenProvider.generateAccessToken(user.id!!)
            val refreshToken = tokenService.issueRefreshToken(user.id!!)
            val expiresIn = jwtTokenProvider.getAccessTokenExpiresInSeconds()

            val response = AuthLoginResponse(
                accessToken = accessToken,
                tokenType = "Bearer",
                expiresIn = expiresIn,
                isNewUser = false,
                user = toAuthUserResponse(user),
            )

            log.info("[AuthService] 관리자 이메일 로그인 처리 완료: userId={}", user.id)
            recordAfterCommit { authMetricsRecorder.recordLogin(provider = "admin-email", result = "success") }

            AuthLoginResult(
                response = response,
                refreshToken = refreshToken,
            )
        }.getOrElse { throwable ->
            authMetricsRecorder.recordLogin(provider = "admin-email", result = "failure")
            throw throwable
        }
    }

    companion object {
        private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$")
    }

    private fun authenticateEmailUser(email: String, rawPassword: String): User {
        val user = userService.findActiveEmailUser(email)
            ?: throw CustomException(ErrorCode.INVALID_CREDENTIALS)

        val passwordHash = user.passwordHash ?: throw CustomException(ErrorCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            throw CustomException(ErrorCode.INVALID_CREDENTIALS)
        }

        return user
    }

    private fun toAuthUserResponse(user: User): AuthUserResponse =
        AuthUserResponse.from(
            user = user,
            email = personalInfoManager.decryptIfNeeded(user.email),
            phoneNumber = personalInfoManager.decryptIfNeeded(user.phoneNumber),
        )

    private fun recordAfterCommit(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action()
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    action()
                }
            },
        )
    }
}
