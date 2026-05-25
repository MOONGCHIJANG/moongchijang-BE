package com.moongchijang.domain.auth.infrastructure.email

import com.moongchijang.domain.auth.application.port.EmailSender
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "ses", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class NoopEmailSender : EmailSender {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(toEmail: String, code: String, expiresInSeconds: Long) {
        log.info(
            "[NoopEmailSender] 발송 provider 설정으로 이메일 발송 생략: toEmail={}, expiresInSeconds={}",
            toEmail,
            expiresInSeconds,
        )
    }
}
