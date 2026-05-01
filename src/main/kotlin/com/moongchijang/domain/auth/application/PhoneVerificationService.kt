package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.PhoneVerificationCodeSendRequest
import com.moongchijang.domain.auth.application.dto.PhoneVerificationCodeSentResponse
import com.moongchijang.domain.auth.application.dto.PhoneVerificationCodeVerifyRequest
import com.moongchijang.domain.auth.application.dto.PhoneVerificationVerifiedResponse
import com.moongchijang.domain.auth.application.port.PhoneVerificationStore
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.domain.auth.infrastructure.sms.coolsms.CoolSmsSender
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.pow

@Service
class PhoneVerificationService(
    private val phoneVerificationStore: PhoneVerificationStore,
    private val coolSmsSender: CoolSmsSender,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendVerificationCode(request: PhoneVerificationCodeSendRequest): PhoneVerificationCodeSentResponse {
        val phoneNumber = normalizePhoneNumber(request.phoneNumber)
        log.info("[PhoneVerificationService] 전화번호 인증코드 발송 시작: phoneNumber={}", maskPhoneNumber(phoneNumber))
        validatePhoneNumber(phoneNumber)

        val code = generateCode()
        val codeHash = hashCode(code)

        phoneVerificationStore.saveCodeHash(phoneNumber, codeHash, CODE_TTL_SECONDS)
        coolSmsSender.sendVerificationCode(phoneNumber, code)
        log.info("[PhoneVerificationService] 전화번호 인증코드 발송 완료: phoneNumber={}", maskPhoneNumber(phoneNumber))

        return PhoneVerificationCodeSentResponse(
            expiresInSeconds = CODE_TTL_SECONDS.toInt(),
            resendAvailableInSeconds = RESEND_AVAILABLE_IN_SECONDS.toInt(),
        )
    }

    fun verifyCode(request: PhoneVerificationCodeVerifyRequest): PhoneVerificationVerifiedResponse {
        val phoneNumber = normalizePhoneNumber(request.phoneNumber)
        log.info("[PhoneVerificationService] 전화번호 인증코드 검증 시작: phoneNumber={}", maskPhoneNumber(phoneNumber))
        validatePhoneNumber(phoneNumber)

        val savedCodeHash = phoneVerificationStore.getCodeHash(phoneNumber)
            ?: throw CustomException(ErrorCode.PHONE_VERIFICATION_CODE_NOT_FOUND)

        val inputCodeHash = hashCode(request.code)
        if (savedCodeHash != inputCodeHash) {
            log.info("[PhoneVerificationService] 전화번호 인증코드 불일치: phoneNumber={}", maskPhoneNumber(phoneNumber))
            throw CustomException(ErrorCode.PHONE_VERIFICATION_CODE_MISMATCH)
        }

        phoneVerificationStore.deleteCode(phoneNumber)
        phoneVerificationStore.markVerified(phoneNumber, VERIFIED_TTL_SECONDS)
        log.info("[PhoneVerificationService] 전화번호 인증코드 검증 완료: phoneNumber={}", maskPhoneNumber(phoneNumber))

        return PhoneVerificationVerifiedResponse(verified = true)
    }

    fun ensureVerified(rawPhoneNumber: String) {
        val phoneNumber = normalizePhoneNumber(rawPhoneNumber)
        validatePhoneNumber(phoneNumber)

        if (!phoneVerificationStore.isVerified(phoneNumber)) {
            throw CustomException(ErrorCode.PHONE_VERIFICATION_REQUIRED)
        }
    }

    private fun normalizePhoneNumber(raw: String): String = raw.replace(Regex("[^0-9]"), "")

    private fun validatePhoneNumber(phoneNumber: String) {
        if (!PHONE_REGEX.matches(phoneNumber)) {
            throw CustomException(ErrorCode.INVALID_PHONE_NUMBER_FORMAT)
        }
    }

    private fun generateCode(): String {
        val bound = 10.0.pow(OTP_LENGTH.toDouble()).toInt()
        val min = bound / 10
        val number = secureRandom.nextInt(bound - min) + min
        return number.toString()
    }

    private fun hashCode(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(code.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun maskPhoneNumber(phoneNumber: String): String {
        return if (phoneNumber.length >= 11) {
            phoneNumber.replace(Regex("(\\d{3})\\d{4}(\\d{4})"), "$1****$2")
        } else {
            "***"
        }
    }

    companion object {
        private const val CODE_TTL_SECONDS = 180L
        private const val VERIFIED_TTL_SECONDS = 1800L
        private const val RESEND_AVAILABLE_IN_SECONDS = 0L
        private const val OTP_LENGTH = 6

        private val PHONE_REGEX = Regex("^01[0-9]\\d{7,8}$")
        private val secureRandom = SecureRandom()
    }
}
