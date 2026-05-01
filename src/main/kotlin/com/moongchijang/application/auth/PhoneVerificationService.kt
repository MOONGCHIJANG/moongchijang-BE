package com.moongchijang.application.auth

import com.moongchijang.application.auth.dto.PhoneVerificationCodeSendRequest
import com.moongchijang.application.auth.dto.PhoneVerificationCodeSentResponse
import com.moongchijang.application.auth.dto.PhoneVerificationCodeVerifyRequest
import com.moongchijang.application.auth.dto.PhoneVerificationVerifiedResponse
import com.moongchijang.application.auth.port.PhoneVerificationStore
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.infrastructure.sms.coolsms.CoolSmsSender
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.pow

@Service
class PhoneVerificationService(
    private val phoneVerificationStore: PhoneVerificationStore,
    private val coolSmsSender: CoolSmsSender,
) {
    fun sendVerificationCode(request: PhoneVerificationCodeSendRequest): PhoneVerificationCodeSentResponse {
        val phoneNumber = normalizePhoneNumber(request.phoneNumber)
        validatePhoneNumber(phoneNumber)

        val code = generateCode()
        val codeHash = hashCode(code)

        phoneVerificationStore.saveCodeHash(phoneNumber, codeHash, CODE_TTL_SECONDS)
        coolSmsSender.sendVerificationCode(phoneNumber, code)

        return PhoneVerificationCodeSentResponse(
            expiresInSeconds = CODE_TTL_SECONDS.toInt(),
            resendAvailableInSeconds = RESEND_AVAILABLE_IN_SECONDS.toInt(),
        )
    }

    fun verifyCode(request: PhoneVerificationCodeVerifyRequest): PhoneVerificationVerifiedResponse {
        val phoneNumber = normalizePhoneNumber(request.phoneNumber)
        validatePhoneNumber(phoneNumber)

        val savedCodeHash = phoneVerificationStore.getCodeHash(phoneNumber)
            ?: throw CustomException(ErrorCode.PHONE_VERIFICATION_CODE_NOT_FOUND)

        val inputCodeHash = hashCode(request.code)
        if (savedCodeHash != inputCodeHash) {
            throw CustomException(ErrorCode.PHONE_VERIFICATION_CODE_MISMATCH)
        }

        phoneVerificationStore.deleteCode(phoneNumber)
        phoneVerificationStore.markVerified(phoneNumber, VERIFIED_TTL_SECONDS)

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

    companion object {
        private const val CODE_TTL_SECONDS = 180L
        private const val VERIFIED_TTL_SECONDS = 1800L
        private const val RESEND_AVAILABLE_IN_SECONDS = 0L
        private const val OTP_LENGTH = 6

        private val PHONE_REGEX = Regex("^01[0-9]\\d{7,8}$")
        private val secureRandom = SecureRandom()
    }
}