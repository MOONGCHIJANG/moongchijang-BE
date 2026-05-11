package com.moongchijang.domain.user.application

import com.moongchijang.domain.auth.application.PhoneVerificationService
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpdatedResponse
import com.moongchijang.domain.user.application.dto.EmailAvailabilityResponse
import com.moongchijang.domain.user.application.dto.NicknameAvailabilityResponse
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.MaskingUtils.maskEmail
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val phoneVerificationService: PhoneVerificationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun findOrCreateKakaoUser(
        providerId: String,
        email: String,
        nickname: String,
    ): Pair<User, Boolean> {
        findActiveKakaoUser(providerId)?.let {
            log.info("[UserService] 기존 카카오 사용자 로그인 처리: userId={}", it.id)
            return it to false
        }
        findDeletedKakaoUser(providerId)?.let {
            val restored = restoreDeletedUser(it, email, nickname)
            log.info("[UserService] 탈퇴 카카오 사용자 복구 처리: userId={}", restored.id)
            return restored to false
        }
        log.info("[UserService] 신규 카카오 사용자 생성 처리")
        return createNewKakaoUser(providerId, email, nickname) to true
    }

    @Transactional
    fun createEmailUser(email: String, passwordHash: String): User {
        val normalizedEmail = normalizeEmail(email)
        log.info("[UserService] 이메일 사용자 생성 시작: email={}", maskEmail(normalizedEmail))
        validateEmailFormat(normalizedEmail)

        if (userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail)) {
            throw CustomException(ErrorCode.DUPLICATE_EMAIL)
        }

        val user = User.newEmailUser(
            email = normalizedEmail,
            passwordHash = passwordHash,
        )
        val savedUser = userRepository.save(user)
        log.info("[UserService] 이메일 사용자 생성 완료: userId={}", savedUser.id)

        return savedUser
    }

    @Transactional(readOnly = true)
    fun findActiveEmailUser(email: String): User? {
        val normalizedEmail = normalizeEmail(email)
        validateEmailFormat(normalizedEmail)

        return userRepository.findByProviderAndEmailAndDeletedAtIsNull(
            provider = AuthProvider.EMAIL,
            email = normalizedEmail,
        )
    }

    @Transactional(readOnly = true)
    fun checkNicknameAvailability(nickname: String): NicknameAvailabilityResponse {
        validateNicknameFormat(nickname)
        val duplicated = userRepository.existsByNicknameAndDeletedAtIsNull(nickname)
        return NicknameAvailabilityResponse(
            nickname = nickname,
            available = !duplicated,
        )
    }

    @Transactional(readOnly = true)
    fun checkEmailAvailability(email: String): EmailAvailabilityResponse {
        val normalizedEmail = normalizeEmail(email)
        log.info("[UserService] 이메일 중복 확인 시작: email={}", maskEmail(normalizedEmail))
        validateEmailFormat(normalizedEmail)

        val duplicated = userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail)
        val response = EmailAvailabilityResponse(
            email = normalizedEmail,
            available = !duplicated,
        )

        log.info(
            "[UserService] 이메일 중복 확인 완료: email={}, available={}",
            maskEmail(normalizedEmail),
            response.available,
        )
        return response
    }

    @Transactional
    fun updateAdditionalInfo(request: AdditionalInfoUpsertRequest, userId: Long): AdditionalInfoUpdatedResponse {
        log.info("[UserService] 추가정보 입력 처리 시작: userId={}", userId)
        validateNicknameFormat(request.nickname)
        validatePhoneNumberFormat(request.phoneNumber)

        phoneVerificationService.ensureVerified(request.phoneNumber)

        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        val duplicated = userRepository.existsByNicknameAndDeletedAtIsNull(request.nickname) &&
            user.nickname != request.nickname
        if (duplicated) {
            throw CustomException(ErrorCode.DUPLICATE_NICKNAME)
        }

        user.completeSignup(request.nickname, request.phoneNumber)
        log.info("[UserService] 추가정보 입력 처리 완료: userId={}", userId)
        return AdditionalInfoUpdatedResponse.from(user)
    }

    private fun findActiveKakaoUser(providerId: String): User? {
        return userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(
            provider = AuthProvider.KAKAO,
            providerId = providerId,
        )
    }

    private fun findDeletedKakaoUser(providerId: String): User? {
        return userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull(
            provider = AuthProvider.KAKAO,
            providerId = providerId,
        )
    }

    private fun restoreDeletedUser(
        deletedUser: User,
        email: String,
        nickname: String,
    ): User {
        val deletedAt = deletedUser.deletedAt
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        validateRejoinAvailable(deletedAt)

        deletedUser.deletedAt = null
        deletedUser.email = email
        if (deletedUser.nickname.isNullOrBlank()) {
            deletedUser.nickname = nickname
        }
        deletedUser.signupCompleted = false

        return deletedUser
    }

    private fun createNewKakaoUser(
        providerId: String,
        email: String,
        nickname: String,
    ): User {
        val newUser = User.newKakaoUser(
            providerId = providerId,
            email = email,
            nickname = nickname,
        )
        val savedUser = userRepository.save(newUser)
        log.info("[UserService] 신규 카카오 사용자 생성 완료: userId={}", savedUser.id)
        return savedUser
    }

    private fun validateRejoinAvailable(deletedAt: LocalDateTime) {
        val rejoinAvailableAt = deletedAt.plusDays(30)
        if (LocalDateTime.now().isBefore(rejoinAvailableAt)) {
            throw CustomException(ErrorCode.REJOIN_NOT_AVAILABLE_YET)
        }
    }

    private fun validateNicknameFormat(nickname: String) {
        val nicknameRegex = Regex("^[A-Za-z0-9가-힣]{2,10}$")
        if (!nicknameRegex.matches(nickname)) {
            throw CustomException(ErrorCode.INVALID_NICKNAME_FORMAT)
        }
    }

    private fun validatePhoneNumberFormat(phoneNumber: String) {
        val phoneRegex = Regex("^01[0-9]-[0-9]{3,4}-[0-9]{4}$")
        if (!phoneRegex.matches(phoneNumber)) {
            throw CustomException(ErrorCode.INVALID_PHONE_NUMBER_FORMAT)
        }
    }

    private fun validateEmailFormat(email: String) {
        if (!EMAIL_REGEX.matches(email)) {
            throw CustomException(ErrorCode.INVALID_EMAIL_FORMAT)
        }
    }

    private fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
