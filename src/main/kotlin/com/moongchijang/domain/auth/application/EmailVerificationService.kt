package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.EmailVerificationCodeSendRequest
import com.moongchijang.domain.auth.application.dto.EmailVerificationCodeSentResponse
import com.moongchijang.domain.auth.application.dto.EmailVerificationCodeVerifyRequest
import com.moongchijang.domain.auth.application.dto.EmailVerificationVerifiedResponse
import com.moongchijang.domain.auth.application.port.EmailSender
import com.moongchijang.domain.auth.application.port.EmailVerificationStore
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.pow

@Service
class EmailVerificationService(
    private val emailVerificationStore: EmailVerificationStore,
    private val emailSender: EmailSender,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendEmailVerificationCode(request: EmailVerificationCodeSendRequest): EmailVerificationCodeSentResponse {
        val email = normalizeEmail(request.email)
        log.info("[EmailVerificationService] 이메일 인증코드 발송 시작: email={}", maskEmail(email))

        val dailyCount = emailVerificationStore.getDailySendCount(email)
        if (dailyCount >= DAILY_LIMIT) {
            throw CustomException(ErrorCode.EMAIL_VERIFICATION_DAILY_LIMIT_EXCEEDED)
        }

        val resendAvailableInSeconds = emailVerificationStore.getResendAvailableInSeconds(email)
        if (resendAvailableInSeconds > 0) {
            throw CustomException(ErrorCode.EMAIL_VERIFICATION_RESEND_COOLDOWN)
        }

        val code = generateCode()
        emailVerificationStore.saveCode(email, code, CODE_EXPIRES_SECONDS)
        emailVerificationStore.setResendCooldown(email, RESEND_COOLDOWN_SECONDS)
        val usedCount = emailVerificationStore.incrementDailySendCount(email)

        emailSender.sendVerificationCode(email, code, CODE_EXPIRES_SECONDS)
        log.info("[EmailVerificationService] 이메일 인증코드 발송 완료: email={}", maskEmail(email))

        return EmailVerificationCodeSentResponse(
            expiresInSeconds = CODE_EXPIRES_SECONDS.toInt(),
            resendAvailableInSeconds = RESEND_COOLDOWN_SECONDS.toInt(),
            remainingDailyAttempts = (DAILY_LIMIT - usedCount).coerceAtLeast(0).toInt(),
        )
    }

    fun verifyEmailVerificationCode(request: EmailVerificationCodeVerifyRequest): EmailVerificationVerifiedResponse {
        val email = normalizeEmail(request.email)
        log.info("[EmailVerificationService] 이메일 인증코드 검증 시작: email={}", maskEmail(email))

        val savedCode = emailVerificationStore.getCode(email)
            ?: throw CustomException(ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED)

        if (savedCode != request.code) {
            log.info("[EmailVerificationService] 이메일 인증코드 불일치: email={}", maskEmail(email))
            throw CustomException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH)
        }

        emailVerificationStore.deleteCode(email)
        log.info("[EmailVerificationService] 이메일 인증코드 검증 완료: email={}", maskEmail(email))

        return EmailVerificationVerifiedResponse(
            verified = true,
            signupToken = UUID.randomUUID().toString(),
        )
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun generateCode(): String {
        val bound = 10.0.pow(OTP_LENGTH.toDouble()).toInt()
        val number = secureRandom.nextInt(bound)
        return number.toString().padStart(OTP_LENGTH, '0')
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"
        val local = parts[0]
        val domain = parts[1]
        val maskedLocal = when {
            local.length <= 2 -> "${local.first()}*"
            else -> local.take(2) + "*".repeat(local.length - 2)
        }
        return "$maskedLocal@$domain"
    }

    companion object {
        private const val CODE_EXPIRES_SECONDS = 180L
        private const val RESEND_COOLDOWN_SECONDS = 60L
        private const val DAILY_LIMIT = 5L
        private const val OTP_LENGTH = 6
        private val secureRandom = SecureRandom()
    }
}
