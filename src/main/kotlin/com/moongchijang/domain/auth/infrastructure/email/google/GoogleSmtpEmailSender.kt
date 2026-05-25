package com.moongchijang.domain.auth.infrastructure.email.google

import com.moongchijang.domain.auth.application.port.EmailSender
import com.moongchijang.global.config.GoogleSmtpProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.MaskingUtils.maskEmail
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "email", name = ["provider"], havingValue = "GOOGLE")
class GoogleSmtpEmailSender(
    private val javaMailSender: JavaMailSender,
    private val googleSmtpProperties: GoogleSmtpProperties,
) : EmailSender {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(toEmail: String, code: String, expiresInSeconds: Long) {
        val expiresMinutes = expiresInSeconds / 60
        val subject = "[뭉치장] 이메일 인증코드를 확인해주세요"
        val bodyText = """
            안녕하세요, 뭉치장입니다.

            이메일 인증코드는 아래와 같습니다.
            $code

            인증코드는 ${expiresMinutes}분간 유효합니다.
        """.trimIndent()

        val message = SimpleMailMessage().apply {
            from = googleSmtpProperties.from
            setTo(toEmail)
            this.subject = subject
            text = bodyText
        }

        try {
            javaMailSender.send(message)
            log.info("[GoogleSmtpEmailSender] 이메일 발송 완료: toEmail={}", maskEmail(toEmail))
        } catch (e: Exception) {
            log.error("[GoogleSmtpEmailSender] 이메일 발송 실패: toEmail={}", maskEmail(toEmail), e)
            throw CustomException(ErrorCode.EMAIL_SEND_FAILED)
        }
    }
}
