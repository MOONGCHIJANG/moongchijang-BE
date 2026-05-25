package com.moongchijang.domain.auth.infrastructure.email.google

import com.moongchijang.global.config.GoogleSmtpProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

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
        googleSmtpProperties = googleSmtpProperties
    )

    @Test
    fun `인증코드 메일 발송 성공`() {
        sender.sendVerificationCode(
            toEmail = "user@example.com",
            code = "123456",
            expiresInSeconds = 180L
        )

        val captor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(javaMailSender, times(1)).send(captor.capture())
        val sentMessage = captor.value

        assertEquals("noreply.moongchijang@gmail.com", sentMessage.from)
        assertEquals("user@example.com", sentMessage.to?.first())
        assertEquals("[뭉치장] 이메일 인증코드를 확인해주세요", sentMessage.subject)
        assert(sentMessage.text?.contains("123456") == true)
        assert(sentMessage.text?.contains("3분간 유효") == true)
    }

    @Test
    fun `인증코드 메일 발송 실패 시 EMAIL_SEND_FAILED 예외`() {
        doThrow(MailSendException("fail"))
            .`when`(javaMailSender)
            .send(any(SimpleMailMessage::class.java))

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
