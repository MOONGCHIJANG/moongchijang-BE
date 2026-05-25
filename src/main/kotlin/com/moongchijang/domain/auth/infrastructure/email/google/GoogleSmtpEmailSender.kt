package com.moongchijang.domain.auth.infrastructure.email.google

import com.moongchijang.domain.auth.application.port.EmailSender
import com.moongchijang.domain.auth.infrastructure.email.VerificationEmailTemplateProvider
import com.moongchijang.global.config.GoogleSmtpProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.MaskingUtils.maskEmail
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "email", name = ["provider"], havingValue = "GOOGLE")
class GoogleSmtpEmailSender(
    private val javaMailSender: JavaMailSender,
    private val googleSmtpProperties: GoogleSmtpProperties,
    private val verificationEmailTemplateProvider: VerificationEmailTemplateProvider,
) : EmailSender {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(toEmail: String, code: String, expiresInSeconds: Long) {
        val template = verificationEmailTemplateProvider.build(code, expiresInSeconds)

        try {
            val mimeMessage = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
            helper.setFrom(googleSmtpProperties.from)
            helper.setTo(toEmail)
            helper.setSubject(template.subject)
            helper.setText(template.bodyText, template.bodyHtml)

            javaMailSender.send(mimeMessage)
            log.info("[GoogleSmtpEmailSender] 이메일 발송 완료: toEmail={}", maskEmail(toEmail))
        } catch (e: Exception) {
            log.error("[GoogleSmtpEmailSender] 이메일 발송 실패: toEmail={}", maskEmail(toEmail), e)
            throw CustomException(ErrorCode.EMAIL_SEND_FAILED)
        }
    }
}
