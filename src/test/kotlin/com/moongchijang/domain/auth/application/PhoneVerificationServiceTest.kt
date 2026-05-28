package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.PhoneVerificationCodeSendRequest
import com.moongchijang.domain.auth.application.dto.PhoneVerificationCodeVerifyRequest
import com.moongchijang.domain.auth.application.port.PhoneVerificationStore
import com.moongchijang.domain.auth.infrastructure.sms.coolsms.CoolSmsSender
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

class PhoneVerificationServiceTest {

    private val phoneVerificationStore: PhoneVerificationStore = Mockito.mock(PhoneVerificationStore::class.java)
    private val coolSmsSender: CoolSmsSender = Mockito.mock(CoolSmsSender::class.java)

    private val phoneVerificationService = PhoneVerificationService(
        phoneVerificationStore = phoneVerificationStore,
        coolSmsSender = coolSmsSender,
    )

    @Test
    fun `인증코드 발송 시 코드 해시 저장 및 문자 발송 호출`() {
        val request = PhoneVerificationCodeSendRequest(phoneNumber = "010-1234-5678")
        val savedPhoneNumber = AtomicReference<String>()
        val savedCodeHash = AtomicReference<String>()
        val sentPhoneNumber = AtomicReference<String>()
        val sentCode = AtomicReference<String>()

        Mockito.doAnswer { invocation ->
            savedPhoneNumber.set(invocation.getArgument(0))
            savedCodeHash.set(invocation.getArgument(1))
            null
        }.`when`(phoneVerificationStore).saveCodeHash(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())

        Mockito.doAnswer { invocation ->
            sentPhoneNumber.set(invocation.getArgument(0))
            sentCode.set(invocation.getArgument(1))
            null
        }.`when`(coolSmsSender).sendVerificationCode(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())

        val response = phoneVerificationService.sendVerificationCode(request)

        Assertions.assertEquals(180, response.expiresInSeconds)
        Assertions.assertEquals(0, response.resendAvailableInSeconds)

        Mockito.verify(phoneVerificationStore).saveCodeHash(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())
        Mockito.verify(coolSmsSender).sendVerificationCode(Mockito.anyString(), Mockito.anyString(), Mockito.eq(180L))

        Assertions.assertEquals("01012345678", savedPhoneNumber.get())
        Assertions.assertEquals("01012345678", sentPhoneNumber.get())
        Assertions.assertTrue(sentCode.get().matches(Regex("^[0-9]{6}$")))
        Assertions.assertEquals(sha256("01012345678", sentCode.get()), savedCodeHash.get())
    }

    @Test
    fun `인증코드 검증 시 저장된 코드 부재 예외`() {
        Mockito.`when`(phoneVerificationStore.getCodeHash("01012345678")).thenReturn(null)

        val exception = assertThrows<CustomException> {
            phoneVerificationService.verifyCode(
                PhoneVerificationCodeVerifyRequest(
                    phoneNumber = "010-1234-5678",
                    code = "123456",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.PHONE_VERIFICATION_CODE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `인증코드 검증 시 코드 불일치 예외`() {
        Mockito.`when`(phoneVerificationStore.getCodeHash("01012345678")).thenReturn(sha256("01012345678", "111111"))

        val exception = assertThrows<CustomException> {
            phoneVerificationService.verifyCode(
                PhoneVerificationCodeVerifyRequest(
                    phoneNumber = "010-1234-5678",
                    code = "222222",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.PHONE_VERIFICATION_CODE_MISMATCH, exception.errorCode)
    }

    @Test
    fun `인증코드 검증 성공 시 코드 삭제 및 인증 완료 저장`() {
        Mockito.`when`(phoneVerificationStore.getCodeHash("01012345678")).thenReturn(sha256("01012345678", "123456"))

        val response = phoneVerificationService.verifyCode(
            PhoneVerificationCodeVerifyRequest(
                phoneNumber = "010-1234-5678",
                code = "123456",
            )
        )

        Assertions.assertTrue(response.verified)
        Mockito.verify(phoneVerificationStore).deleteCode("01012345678")
        Mockito.verify(phoneVerificationStore).markVerified("01012345678", 1800L)
    }

    @Test
    fun `인증 완료 여부 확인 시 미인증 예외`() {
        Mockito.`when`(phoneVerificationStore.isVerified("01012345678")).thenReturn(false)

        val exception = assertThrows<CustomException> {
            phoneVerificationService.ensureVerified("010-1234-5678")
        }

        Assertions.assertEquals(ErrorCode.PHONE_VERIFICATION_REQUIRED, exception.errorCode)
    }

    @Test
    fun `인증 완료 여부 확인 시 인증 상태 통과`() {
        Mockito.`when`(phoneVerificationStore.isVerified("01012345678")).thenReturn(true)

        phoneVerificationService.ensureVerified("010-1234-5678")

        Mockito.verify(phoneVerificationStore).isVerified("01012345678")
    }

    private fun sha256(phoneNumber: String, code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$phoneNumber:$code".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
