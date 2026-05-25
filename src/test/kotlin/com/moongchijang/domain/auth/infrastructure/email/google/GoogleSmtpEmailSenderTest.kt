package com.moongchijang.domain.auth.infrastructure.email.google

import com.moongchijang.domain.auth.infrastructure.email.VerificationEmailTemplateProvider
import com.moongchijang.global.config.GoogleSmtpProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import java.util.Properties

class GoogleSmtpEmailSenderTest {

    private val javaMailSender: JavaMailSender = mock(JavaMailSender::class.java)
    private val googleSmtpProperties = GoogleSmtpProperties(
        host = "smtp.gmail.com",
        port = 587,
        username = "noreply.moongchijang@gmail.com",
        appPassword = "app-password",
        from = "noreply.moongchijang@gmail.com"
    )

    private val sender = GoogleSmtpEmailSender(
        javaMailSender = javaMailSender,
        googleSmtpProperties = googleSmtpProperties,
        verificationEmailTemplateProvider = VerificationEmailTemplateProvider()
    )

    @Test
    fun `인증코드 메일 발송 성공`() {
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        doReturn(mimeMessage).`when`(javaMailSender).createMimeMessage()

        sender.sendVerificationCode(
            toEmail = "user@example.com",
            code = "123456",
            expiresInSeconds = 180L
        )

        verify(javaMailSender, times(1)).send(mimeMessage)
        assertEquals("[뭉치장] 이메일 인증코드를 확인해주세요", mimeMessage.subject)
    }

    @Test
    fun `인증코드 메일 발송 실패 시 EMAIL_SEND_FAILED 예외`() {
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        doReturn(mimeMessage).`when`(javaMailSender).createMimeMessage()

        doThrow(MailSendException("fail"))
            .`when`(javaMailSender)
            .send(any(MimeMessage::class.java))

        val ex = assertThrows<CustomException> {
            sender.sendVerificationCode(
                toEmail = "user@example.com",
                code = "123456",
                expiresInSeconds = 180L
            )
        }

        assertEquals(ErrorCode.EMAIL_SEND_FAILED, ex.errorCode)
    }
}
