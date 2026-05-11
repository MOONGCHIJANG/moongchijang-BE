package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.EmailVerificationCodeSendRequest
import com.moongchijang.domain.auth.application.dto.EmailVerificationCodeVerifyRequest
import com.moongchijang.domain.auth.application.port.EmailSender
import com.moongchijang.domain.auth.application.port.EmailSignupTokenStore
import com.moongchijang.domain.auth.application.port.EmailVerificationStore
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.concurrent.atomic.AtomicReference

class EmailVerificationServiceTest {

    private val emailVerificationStore: EmailVerificationStore = Mockito.mock(EmailVerificationStore::class.java)
    private val emailSignupTokenStore: EmailSignupTokenStore = Mockito.mock(EmailSignupTokenStore::class.java)
    private val emailSender: EmailSender = Mockito.mock(EmailSender::class.java)

    private val emailVerificationService = EmailVerificationService(
        emailVerificationStore = emailVerificationStore,
        emailSignupTokenStore = emailSignupTokenStore,
        emailSender = emailSender,
    )

    @Test
    fun `이메일 인증코드 발송 성공 시 저장 및 발송 호출`() {
        val request = EmailVerificationCodeSendRequest(email = "new@example.com")
        val savedEmail = AtomicReference<String>()
        val savedCode = AtomicReference<String>()
        val sentEmail = AtomicReference<String>()
        val sentCode = AtomicReference<String>()

        Mockito.`when`(emailVerificationStore.getDailySendCount("new@example.com")).thenReturn(0L)
        Mockito.`when`(emailVerificationStore.getResendAvailableInSeconds("new@example.com")).thenReturn(0L)
        Mockito.`when`(emailVerificationStore.incrementDailySendCount("new@example.com")).thenReturn(1L)

        Mockito.doAnswer { invocation ->
            savedEmail.set(invocation.getArgument(0))
            savedCode.set(invocation.getArgument(1))
            null
        }.`when`(emailVerificationStore).saveCode(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())

        Mockito.doAnswer { invocation ->
            sentEmail.set(invocation.getArgument(0))
            sentCode.set(invocation.getArgument(1))
            null
        }.`when`(emailSender).sendVerificationCode(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())

        val response = emailVerificationService.sendEmailVerificationCode(request)

        Assertions.assertEquals(180, response.expiresInSeconds)
        Assertions.assertEquals(60, response.resendAvailableInSeconds)
        Assertions.assertEquals(4, response.remainingDailyAttempts)

        Mockito.verify(emailVerificationStore).saveCode(Mockito.anyString(), Mockito.anyString(), Mockito.eq(180L))
        Mockito.verify(emailVerificationStore).setResendCooldown("new@example.com", 60L)
        Mockito.verify(emailSender).sendVerificationCode(Mockito.anyString(), Mockito.anyString(), Mockito.eq(180L))

        Assertions.assertEquals("new@example.com", savedEmail.get())
        Assertions.assertEquals("new@example.com", sentEmail.get())
        Assertions.assertTrue(savedCode.get().matches(Regex("^[0-9]{6}$")))
        Assertions.assertTrue(sentCode.get().matches(Regex("^[0-9]{6}$")))
        Assertions.assertEquals(savedCode.get(), sentCode.get())
    }

    @Test
    fun `이메일 인증코드 발송 시 재발송 쿨다운이면 예외`() {
        Mockito.`when`(emailVerificationStore.getDailySendCount("new@example.com")).thenReturn(0L)
        Mockito.`when`(emailVerificationStore.getResendAvailableInSeconds("new@example.com")).thenReturn(30L)

        val exception = assertThrows<CustomException> {
            emailVerificationService.sendEmailVerificationCode(
                EmailVerificationCodeSendRequest(email = "new@example.com")
            )
        }

        Assertions.assertEquals(ErrorCode.EMAIL_VERIFICATION_RESEND_COOLDOWN, exception.errorCode)
    }

    @Test
    fun `이메일 인증코드 발송 시 일일 한도 초과면 예외`() {
        Mockito.`when`(emailVerificationStore.getDailySendCount("new@example.com")).thenReturn(5L)

        val exception = assertThrows<CustomException> {
            emailVerificationService.sendEmailVerificationCode(
                EmailVerificationCodeSendRequest(email = "new@example.com")
            )
        }

        Assertions.assertEquals(ErrorCode.EMAIL_VERIFICATION_DAILY_LIMIT_EXCEEDED, exception.errorCode)
    }

    @Test
    fun `이메일 인증코드 검증 성공 시 코드 삭제 및 signupToken 저장`() {
        Mockito.`when`(emailVerificationStore.getCode("new@example.com")).thenReturn("123456")
        val savedEmail = AtomicReference<String>()
        val savedSignupToken = AtomicReference<String>()
        val savedTtl = AtomicReference<Long>()

        Mockito.doAnswer { invocation ->
            savedEmail.set(invocation.getArgument(0))
            savedSignupToken.set(invocation.getArgument(1))
            savedTtl.set(invocation.getArgument(2))
            null
        }.`when`(emailSignupTokenStore).save(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())

        val response = emailVerificationService.verifyEmailVerificationCode(
            EmailVerificationCodeVerifyRequest(
                email = "new@example.com",
                code = "123456",
            )
        )

        Assertions.assertTrue(response.verified)
        Assertions.assertFalse(response.signupToken.isBlank())
        Mockito.verify(emailVerificationStore).deleteCode("new@example.com")
        Mockito.verify(emailSignupTokenStore).save(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())
        Assertions.assertEquals("new@example.com", savedEmail.get())
        Assertions.assertEquals(response.signupToken, savedSignupToken.get())
        Assertions.assertEquals(1800L, savedTtl.get())
    }

    @Test
    fun `이메일 인증코드 검증 시 코드 만료면 예외`() {
        Mockito.`when`(emailVerificationStore.getCode("new@example.com")).thenReturn(null)

        val exception = assertThrows<CustomException> {
            emailVerificationService.verifyEmailVerificationCode(
                EmailVerificationCodeVerifyRequest(
                    email = "new@example.com",
                    code = "123456",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED, exception.errorCode)
    }

    @Test
    fun `이메일 인증코드 검증 시 코드 불일치면 예외`() {
        Mockito.`when`(emailVerificationStore.getCode("new@example.com")).thenReturn("111111")

        val exception = assertThrows<CustomException> {
            emailVerificationService.verifyEmailVerificationCode(
                EmailVerificationCodeVerifyRequest(
                    email = "new@example.com",
                    code = "222222",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH, exception.errorCode)
    }
}
