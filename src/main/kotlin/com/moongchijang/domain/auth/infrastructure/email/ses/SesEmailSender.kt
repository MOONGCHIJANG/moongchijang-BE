package com.moongchijang.domain.auth.infrastructure.email.ses

import com.moongchijang.domain.auth.application.port.EmailSender
import com.moongchijang.global.config.SesProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.MaskingUtils.maskEmail
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sesv2.SesV2Client
import software.amazon.awssdk.services.sesv2.model.Body
import software.amazon.awssdk.services.sesv2.model.Content
import software.amazon.awssdk.services.sesv2.model.Destination
import software.amazon.awssdk.services.sesv2.model.EmailContent
import software.amazon.awssdk.services.sesv2.model.Message
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest

@Component
@ConditionalOnProperty(prefix = "ses", name = ["enabled"], havingValue = "true")
class SesEmailSender(
    private val sesV2Client: SesV2Client,
    private val sesProperties: SesProperties,
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

        val request = SendEmailRequest.builder()
            .fromEmailAddress(sesProperties.fromEmail)
            .destination(
                Destination.builder()
                    .toAddresses(toEmail)
                    .build(),
            )
            .content(
                EmailContent.builder()
                    .simple(
                        Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(
                                Body.builder()
                                    .text(Content.builder().data(bodyText).charset("UTF-8").build())
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .build()

        try {
            sesV2Client.sendEmail(request)
            log.info("[SesEmailSender] 이메일 발송 완료: toEmail={}", maskEmail(toEmail))
        } catch (e: Exception) {
            log.error("[SesEmailSender] 이메일 발송 실패: toEmail={}", maskEmail(toEmail), e)
            throw CustomException(ErrorCode.EMAIL_SEND_FAILED)
        }
    }
}
