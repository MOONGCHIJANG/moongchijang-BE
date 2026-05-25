package com.moongchijang.domain.auth.infrastructure.email

import org.springframework.stereotype.Component

@Component
class VerificationEmailTemplateProvider {

    fun build(code: String, expiresInSeconds: Long): VerificationEmailTemplate {
        val expiresMinutes = expiresInSeconds / 60
        val subject = "[뭉치장] 이메일 인증코드를 확인해주세요"
        val bodyText = """
            안녕하세요, 뭉치장입니다.

            이메일 인증코드는 아래와 같습니다.
            $code

            인증코드는 ${expiresMinutes}분간 유효합니다.
        """.trimIndent()

        return VerificationEmailTemplate(
            subject = subject,
            bodyText = bodyText
        )
    }
}

data class VerificationEmailTemplate(
    val subject: String,
    val bodyText: String,
)
